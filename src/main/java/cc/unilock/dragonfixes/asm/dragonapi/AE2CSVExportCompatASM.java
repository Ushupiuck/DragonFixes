package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * Applied Energistics 2 starts an optional CSV-export thread in init() which
 * enumerates all mod items before ChromatiCraft's research classes are ready.
 * Replace AppEng.startService's Thread.start() call with a bridge that defers
 * only the CSV thread until FMLLoadCompleteEvent. All other services still start.
 *
 * ASM avoids pulling GTNH's custom AE2 fork into DragonFixes' compile path.
 */
public final class AE2CSVExportCompatASM implements IClassTransformer {
    private static final String TARGET = "appeng.core.AppEng";
    private static final String BRIDGE = "cc/unilock/dragonfixes/compat/AE2CSVExportBridge";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)
                || Launch.classLoader.getResource(
                        "Reika/ChromatiCraft/Magic/Progression/ChromaResearchManager.class") == null) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (!"startService".equals(method.name)
                    || !"(Ljava/lang/String;Ljava/lang/Thread;)V".equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "java/lang/Thread".equals(call.owner)
                        && "start".equals(call.name)
                        && "()V".equals(call.desc)) {
                    // Original stack: [thread]. Append the method's service name
                    // and pass both to DragonFixes' optional thread scheduler.
                    method.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 1));
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = BRIDGE;
                    call.name = "startOrDefer";
                    call.desc = "(Ljava/lang/Thread;Ljava/lang/String;)V";
                    call.itf = false;
                    changes++;
                }
            }
        }
        if (changes != 1) {
            DragonFixes.LOGGER.error("Expected one AE2 AppEng.startService Thread.start, found {}; "
                    + "leaving original class unchanged", changes);
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        DragonFixes.LOGGER.info("Enabled late-start compatibility for AE2 CSV item export");
        return writer.toByteArray();
    }
}
