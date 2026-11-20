package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cc.unilock.dragonfixes.DragonFixes;

/** Preserve AE2's item CSV export when an ItemStack has a broken display name. */
public final class AE2CSVItemNameCompatASM implements IClassTransformer {
    private static final String TARGET =
            "appeng.services.export.MinecraftItemCSVExporter$TypeExtractFunction";
    private static final String BRIDGE =
            "cc/unilock/dragonfixes/compat/AE2CSVExportBridge";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int replaced = 0;
        for (MethodNode method : node.methods) {
            if (!"apply".equals(method.name)
                    || !"(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;".equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "net/minecraft/item/ItemStack".equals(call.owner)
                        && ("getDisplayName".equals(call.name)
                                || "func_82833_r".equals(call.name))
                        && "()Ljava/lang/String;".equals(call.desc)) {
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = BRIDGE;
                    call.name = "csvDisplayName";
                    call.desc = "(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;";
                    call.itf = false;
                    replaced++;
                }
            }
        }
        if (replaced != 2) {
            DragonFixes.LOGGER.warn("Expected two AE2 CSV TypeExtractFunction display-name "
                    + "calls, found {}; exporter unchanged", replaced);
            return bytes;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        DragonFixes.LOGGER.info("Guarded AE2 CSV export against invalid modded item display names");
        return writer.toByteArray();
    }
}
