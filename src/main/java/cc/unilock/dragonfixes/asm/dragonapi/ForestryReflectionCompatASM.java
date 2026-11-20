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

/**
 * Reika uses Class.getDeclaredField on GTNH Forestry registries but does not
 * set the field accessible before Field.get. Some entries are private final.
 */
public final class ForestryReflectionCompatASM implements IClassTransformer {
    private static final String TARGET =
            "Reika.DragonAPI.ModInteract.ItemHandlers.ForestryHandler";
    private static final String BRIDGE =
            "cc/unilock/dragonfixes/compat/ForestryReflectionBridge";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changed = 0;
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "java/lang/Class".equals(call.owner)
                        && "getDeclaredField".equals(call.name)
                        && "(Ljava/lang/String;)Ljava/lang/reflect/Field;".equals(call.desc)) {
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = BRIDGE;
                    call.name = "getAccessibleDeclaredField";
                    call.desc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;";
                    call.itf = false;
                    changed++;
                }
            }
        }
        if (changed < 2) {
            DragonFixes.LOGGER.warn("Expected Forestry registry-field lookups; "
                    + "found {}, leaving class unchanged", changed);
            return bytes;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        DragonFixes.LOGGER.info("Enabled private Forestry field access for DragonAPI ({} lookups)",
                changed);
        return writer.toByteArray();
    }
}
