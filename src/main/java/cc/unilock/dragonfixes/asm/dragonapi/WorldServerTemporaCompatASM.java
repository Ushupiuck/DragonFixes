package cc.unilock.dragonfixes.asm.dragonapi;

import java.net.URL;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.classloading.FMLForgePlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * DragonAPI rewrites Block.updateTick into BlockTickEvent.fire before Tempora's
 * @WrapOperation is applied. Restore only the scheduled tick invocation, then
 * let both mods wrap it via MixinExtras (see MixinWorldServerTemporaCompat).
 */
public class WorldServerTemporaCompatASM implements IClassTransformer {
    private static final String WORLD_SERVER = "net.minecraft.world.WorldServer";
    private static final String TICK_EVENT = "Reika/DragonAPI/Instantiable/Event/BlockTickEvent";
    private static final String FLAGS = TICK_EVENT + "$UpdateFlags";
    private static final String BLOCK = "net/minecraft/block/Block";
    private static final String BLOCK_TICK_DESC =
            "(Lnet/minecraft/world/World;IIILjava/util/Random;)V";
    private static final String EVENT_DESC =
            "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;IIILjava/util/Random;"
                    + "LReika/DragonAPI/Instantiable/Event/BlockTickEvent$UpdateFlags;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !WORLD_SERVER.equals(transformedName)) {
            return bytes;
        }
        URL tempora = Launch.classLoader.getResource("com/colen/tempora/mixins/MixinWorldServer.class");
        if (tempora == null) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changed = 0;
        for (MethodNode method : node.methods) {
            if (!"(Z)Z".equals(method.desc)
                    || !("func_72955_a".equals(method.name) || "tickUpdates".equals(method.name))) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() != Opcodes.INVOKESTATIC || !TICK_EVENT.equals(call.owner)
                        || !"fire".equals(call.name) || !EVENT_DESC.equals(call.desc)) {
                    continue;
                }

                AbstractInsnNode previous = call.getPrevious();
                while (previous != null && previous.getOpcode() < 0) {
                    previous = previous.getPrevious();
                }
                if (!(previous instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode flag = (FieldInsnNode) previous;
                if (flag.getOpcode() != Opcodes.GETSTATIC || !FLAGS.equals(flag.owner)
                        || !"SCHEDULED".equals(flag.name)) {
                    continue;
                }

                method.instructions.remove(flag);
                call.setOpcode(Opcodes.INVOKEVIRTUAL);
                call.owner = BLOCK;
                call.name = FMLForgePlugin.RUNTIME_DEOBF ? "func_149674_a" : "updateTick";
                call.desc = BLOCK_TICK_DESC;
                call.itf = false;
                changed++;
            }
        }
        if (changed != 1) {
            DragonFixes.LOGGER.error("Tempora bridge expected one DragonAPI tick redirect, found {}; "
                    + "WorldServer left unchanged", changed);
            return bytes;
        }

        DragonFixes.LOGGER.info("Restored WorldServer.tickUpdates call for Tempora; "
                + "DragonAPI event preserved by DragonFixes Mixin");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
