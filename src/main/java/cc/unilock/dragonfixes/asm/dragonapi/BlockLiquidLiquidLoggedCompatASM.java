package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.classloading.FMLForgePlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * DragonAPI's Submergeable patch completely replaces BlockLiquid's
 * shouldSideBeRendered body with ReikaBlockHelper.renderLiquidSide.
 * LiquidLogged's client WrapOperation therefore cannot find the original
 * IBlockAccess.getBlock invocation in that method.
 *
 * Restore a real getBlock call as the final argument to our compatibility
 * renderer, so LiquidLogged can wrap the result before rendering is decided.
 * DragonAPI's rendering helper is still called for all other cases.
 */
public class BlockLiquidLiquidLoggedCompatASM implements IClassTransformer {
    private static final String TARGET = "net.minecraft.block.BlockLiquid";
    private static final String HELPER = "Reika/DragonAPI/Libraries/World/ReikaBlockHelper";
    private static final String BRIDGE = "cc/unilock/dragonfixes/compat/BlockLiquidRenderBridge";
    private static final String HELPER_DESC =
            "(Lnet/minecraft/world/IBlockAccess;IIIILnet/minecraft/block/Block;)Z";
    private static final String BRIDGE_DESC =
            "(Lnet/minecraft/world/IBlockAccess;IIIILnet/minecraft/block/Block;"
                    + "Lnet/minecraft/block/Block;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)
                || Launch.classLoader.getResource(
                        "com/gtnewhorizons/liquidlogged/mixins/early/minecraft/client/"
                                + "MixinBlockLiquid_Client.class") == null) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changed = 0;
        for (MethodNode method : node.methods) {
            if (!"(Lnet/minecraft/world/IBlockAccess;IIII)Z".equals(method.desc)
                    || !("func_149646_a".equals(method.name)
                            || "shouldSideBeRendered".equals(method.name))) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() != Opcodes.INVOKESTATIC || !HELPER.equals(call.owner)
                        || !"renderLiquidSide".equals(call.name)
                        || !HELPER_DESC.equals(call.desc)) {
                    continue;
                }

                // The original helper's six arguments are already on the stack.
                // This extra lookup becomes the 7th argument, and is the exact
                // call site that LiquidLogged's MixinExtras wrapper expects.
                InsnList neighbor = new InsnList();
                neighbor.add(new VarInsnNode(Opcodes.ALOAD, 1));
                neighbor.add(new VarInsnNode(Opcodes.ILOAD, 2));
                neighbor.add(new VarInsnNode(Opcodes.ILOAD, 3));
                neighbor.add(new VarInsnNode(Opcodes.ILOAD, 4));
                neighbor.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                        "net/minecraft/world/IBlockAccess",
                        FMLForgePlugin.RUNTIME_DEOBF ? "func_147439_a" : "getBlock",
                        "(III)Lnet/minecraft/block/Block;", true));
                method.instructions.insertBefore(call, neighbor);

                call.owner = BRIDGE;
                call.desc = BRIDGE_DESC;
                changed++;
            }
        }

        if (changed != 1) {
            DragonFixes.LOGGER.error("LiquidLogged bridge expected one DragonAPI "
                    + "renderLiquidSide call, found {}; BlockLiquid left unchanged", changed);
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        DragonFixes.LOGGER.info("Restored BlockLiquid.shouldSideBeRendered getBlock call "
                + "for LiquidLogged while preserving DragonAPI's liquid rendering");
        return writer.toByteArray();
    }
}
