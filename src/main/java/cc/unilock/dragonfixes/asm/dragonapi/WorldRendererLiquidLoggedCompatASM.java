package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.classloading.FMLForgePlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * Reika's RenderBlockEvent rewrites three vanilla render calls into static
 * RenderBlockAtPosEvent methods. LiquidLogged 0.2.5 wraps those original calls
 * in WorldRenderer.updateRenderer and fails to apply when they disappear.
 *
 * Restore the real calls as inputs to compatibility methods. For rendering,
 * keep Reika's event as the primary path, and provide a second (air-block)
 * RenderBlocks invocation only where LiquidLogged has a stored fluid.
 *
 * The liquidlogged$fluidBlock field is injected by LiquidLogged's own mixin.
 */
public class WorldRendererLiquidLoggedCompatASM implements IClassTransformer {
    private static final String WORLD_RENDERER = "net.minecraft.client.renderer.WorldRenderer";
    private static final String EVENT =
            "Reika/DragonAPI/Instantiable/Event/Client/RenderBlockAtPosEvent";
    private static final String BRIDGE =
            "cc/unilock/dragonfixes/compat/WorldRendererRenderBridge";
    private static final String BLOCK = "net/minecraft/block/Block";
    private static final String RENDER_BLOCKS = "net/minecraft/client/renderer/RenderBlocks";
    private static final String BLOCK_DESC = "Lnet/minecraft/block/Block;";
    private static final String PASS_EVENT_DESC = "(" + BLOCK_DESC + "III)I";
    private static final String CHECK_EVENT_DESC = "(" + BLOCK_DESC + "IIII)Z";
    private static final String RENDER_EVENT_DESC = "(Lnet/minecraft/client/renderer/RenderBlocks;"
            + BLOCK_DESC + "IIILnet/minecraft/client/renderer/WorldRenderer;I)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !WORLD_RENDERER.equals(transformedName)
                || Launch.classLoader.getResource(
                        "com/gtnewhorizons/liquidlogged/mixins/early/minecraft/client/"
                        + "MixinWorldRenderer.class") == null) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int maxPass = 0;
        int canRender = 0;
        int rendered = 0;

        for (MethodNode method : node.methods) {
            if (!"(Lnet/minecraft/entity/EntityLivingBase;)V".equals(method.desc)
                    || !("func_147892_a".equals(method.name) || "updateRenderer".equals(method.name))) {
                continue;
            }

            // Reserved across all three transformations, but scoped to this method.
            final int rb = method.maxLocals;
            final int block = rb + 1;
            final int x = rb + 2;
            final int y = rb + 3;
            final int z = rb + 4;
            final int renderer = rb + 5;
            final int pass = rb + 6;
            final int result = rb + 7;

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() != Opcodes.INVOKESTATIC || !EVENT.equals(call.owner)) {
                    continue;
                }

                if ("getMaxRenderPass".equals(call.name)
                        && PASS_EVENT_DESC.equals(call.desc)) {
                    // [block, x, y, z] -> [block, augmentedPass, x, y, z].
                    InsnList before = new InsnList();
                    before.add(new VarInsnNode(Opcodes.ISTORE, z));
                    before.add(new VarInsnNode(Opcodes.ISTORE, y));
                    before.add(new VarInsnNode(Opcodes.ISTORE, x));
                    before.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP));
                    before.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BLOCK,
                            FMLForgePlugin.RUNTIME_DEOBF ? "func_149701_w" : "getRenderBlockPass",
                            "()I", false));
                    before.add(new VarInsnNode(Opcodes.ILOAD, x));
                    before.add(new VarInsnNode(Opcodes.ILOAD, y));
                    before.add(new VarInsnNode(Opcodes.ILOAD, z));
                    method.instructions.insertBefore(call, before);

                    call.owner = BRIDGE;
                    call.name = "getMaxRenderPass";
                    call.desc = "(" + BLOCK_DESC + "IIII)I";
                    maxPass++;
                } else if ("checkCanRenderPass".equals(call.name)
                        && CHECK_EVENT_DESC.equals(call.desc)) {
                    // [block, pass, x, y, z] -> [block, pass, fluidAware, x, y, z].
                    InsnList before = new InsnList();
                    before.add(new VarInsnNode(Opcodes.ISTORE, z));
                    before.add(new VarInsnNode(Opcodes.ISTORE, y));
                    before.add(new VarInsnNode(Opcodes.ISTORE, x));
                    before.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP2));
                    before.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BLOCK,
                            "canRenderInPass", "(I)Z", false));
                    before.add(new VarInsnNode(Opcodes.ILOAD, x));
                    before.add(new VarInsnNode(Opcodes.ILOAD, y));
                    before.add(new VarInsnNode(Opcodes.ILOAD, z));
                    method.instructions.insertBefore(call, before);

                    call.owner = BRIDGE;
                    call.name = "checkCanRenderPass";
                    call.desc = "(" + BLOCK_DESC + "IZIII)Z";
                    canRender++;
                } else if ("fire".equals(call.name) && RENDER_EVENT_DESC.equals(call.desc)) {
                    // Save the original call arguments. Reika's watchers still
                    // decide whether the regular block is drawn.
                    InsnList before = new InsnList();
                    before.add(new VarInsnNode(Opcodes.ISTORE, pass));
                    before.add(new VarInsnNode(Opcodes.ASTORE, renderer));
                    before.add(new VarInsnNode(Opcodes.ISTORE, z));
                    before.add(new VarInsnNode(Opcodes.ISTORE, y));
                    before.add(new VarInsnNode(Opcodes.ISTORE, x));
                    before.add(new VarInsnNode(Opcodes.ASTORE, block));
                    before.add(new VarInsnNode(Opcodes.ASTORE, rb));
                    before.add(new VarInsnNode(Opcodes.ALOAD, rb));
                    before.add(new VarInsnNode(Opcodes.ALOAD, block));
                    before.add(new VarInsnNode(Opcodes.ILOAD, x));
                    before.add(new VarInsnNode(Opcodes.ILOAD, y));
                    before.add(new VarInsnNode(Opcodes.ILOAD, z));
                    before.add(new VarInsnNode(Opcodes.ALOAD, renderer));
                    before.add(new VarInsnNode(Opcodes.ILOAD, pass));
                    method.instructions.insertBefore(call, before);
                    call.owner = BRIDGE;
                    call.name = "fireBlockIfInPass";

                    // LiquidLogged injects this unique field into WorldRenderer.
                    // Avoid a second render call for ordinary, non-fluid cells.
                    LabelNode noFluid = new LabelNode();
                    InsnList after = new InsnList();
                    after.add(new VarInsnNode(Opcodes.ISTORE, result));
                    after.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    after.add(new FieldInsnNode(Opcodes.GETFIELD, WORLD_RENDERER.replace('.', '/'),
                            "liquidlogged$fluidBlock", BLOCK_DESC));
                    after.add(new JumpInsnNode(Opcodes.IFNULL, noFluid));
                    after.add(new VarInsnNode(Opcodes.ALOAD, rb));
                    after.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/init/Blocks",
                            FMLForgePlugin.RUNTIME_DEOBF ? "field_150350_a" : "air", BLOCK_DESC));
                    after.add(new VarInsnNode(Opcodes.ILOAD, x));
                    after.add(new VarInsnNode(Opcodes.ILOAD, y));
                    after.add(new VarInsnNode(Opcodes.ILOAD, z));
                    after.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, RENDER_BLOCKS,
                            FMLForgePlugin.RUNTIME_DEOBF
                                    ? "func_147805_b" : "renderBlockByRenderType",
                            "(" + BLOCK_DESC + "III)Z", false));
                    after.add(new VarInsnNode(Opcodes.ILOAD, result));
                    after.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IOR));
                    after.add(new VarInsnNode(Opcodes.ISTORE, result));
                    after.add(noFluid);
                    after.add(new VarInsnNode(Opcodes.ILOAD, result));
                    method.instructions.insert(call, after);
                    rendered++;
                }
            }
            method.maxLocals = rb + 8;
        }

        if (maxPass != 1 || canRender != 1 || rendered != 1) {
            DragonFixes.LOGGER.error(
                    "LiquidLogged WorldRenderer bridge expected 1/1/1 Reika redirects, "
                            + "found {}/{}/{}; leaving class unchanged",
                    maxPass, canRender, rendered);
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        DragonFixes.LOGGER.info("Restored WorldRenderer render pass, pass check and "
                + "stored-fluid draw calls for LiquidLogged with DragonAPI events intact");
        return writer.toByteArray();
    }
}
