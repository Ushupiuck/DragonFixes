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
 * DragonAPI's ore dictionary validation logs ItemStack.toString(). During
 * GT++ preInit, BlockBaseModular can register an ItemBlock before its material
 * has been initialized, causing a diagnostic string to throw an NPE.
 *
 * Change only the diagnostic StringBuilder.append(Object) call sites inside
 * catchNullOreDict. The original null checks, event registration, and
 * de-duplication logic all remain unchanged.
 */
public final class ReikaOreDictionaryLogASM implements IClassTransformer {
    private static final String TARGET = "Reika.DragonAPI.Auxiliary.DragonAPIEventWatcher";
    private static final String BRIDGE = "cc/unilock/dragonfixes/compat/ReikaOreDictionaryLogBridge";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int fixed = 0;
        for (MethodNode method : node.methods) {
            if (!"catchNullOreDict".equals(method.name)
                    || !"(Lnet/minecraftforge/oredict/OreDictionary$OreRegisterEvent;)V"
                            .equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "java/lang/StringBuilder".equals(call.owner)
                        && "append".equals(call.name)
                        && "(Ljava/lang/Object;)Ljava/lang/StringBuilder;".equals(call.desc)) {
                    // Retains stack effect and allows existing diagnostics to
                    // render even when an ItemStack's block is half-constructed.
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = BRIDGE;
                    call.name = "appendSafely";
                    call.desc = "(Ljava/lang/StringBuilder;Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                    call.itf = false;
                    fixed++;
                }
            }
        }
        if (fixed == 0) {
            DragonFixes.LOGGER.warn("Could not find DragonAPI OreDict logging call sites; leaving them unchanged");
            return bytes;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        DragonFixes.LOGGER.info("Guarded {} DragonAPI OreDict diagnostic formatting call(s)", fixed);
        return writer.toByteArray();
    }
}
