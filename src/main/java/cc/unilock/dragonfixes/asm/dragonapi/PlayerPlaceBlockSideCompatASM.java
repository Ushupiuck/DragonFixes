package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * GuideNH/StructureLib's simulated multiblock construction calls
 * ItemBlock.placeBlockAt with side=6 (ForgeDirection.UNKNOWN). DragonAPI's
 * PlayerPlaceBlockEvent directly indexes VALID_DIRECTIONS[side], whose
 * length is only 6, causing 100+ multiblock preview failures.
 *
 * Replace that one AALOAD with a bounds-safe lookup; all valid 0..5
 * directions and cancellable placement events keep their original behavior.
 */
public final class PlayerPlaceBlockSideCompatASM implements IClassTransformer {
    private static final String TARGET =
            "Reika.DragonAPI.Instantiable.Event.PlayerPlaceBlockEvent";
    private static final String DIRECTION =
            "net/minecraftforge/common/util/ForgeDirection";
    private static final String BRIDGE =
            "cc/unilock/dragonfixes/compat/PlayerPlaceBlockSideBridge";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.getOpcode() != Opcodes.GETSTATIC
                        || !DIRECTION.equals(field.owner)
                        || !"VALID_DIRECTIONS".equals(field.name)) {
                    continue;
                }

                // Expected pattern: GETSTATIC VALID_DIRECTIONS, ILOAD side, AALOAD.
                AbstractInsnNode index = field.getNext();
                while (index != null && index.getOpcode() < 0) index = index.getNext();
                if (index == null || index.getOpcode() != Opcodes.ILOAD) {
                    continue;
                }
                AbstractInsnNode arrayLoad = index.getNext();
                while (arrayLoad != null && arrayLoad.getOpcode() < 0) {
                    arrayLoad = arrayLoad.getNext();
                }
                if (arrayLoad != null && arrayLoad.getOpcode() == Opcodes.AALOAD) {
                    method.instructions.set(arrayLoad, new MethodInsnNode(
                            Opcodes.INVOKESTATIC, BRIDGE, "lookup",
                            "([L" + DIRECTION + ";I)L" + DIRECTION + ";", false));
                    patched++;
                }
            }
        }
        if (patched != 1) {
            DragonFixes.LOGGER.warn("Expected one PlayerPlaceBlockEvent side lookup, "
                    + "found {}; leaving class unchanged", patched);
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        DragonFixes.LOGGER.info("Made DragonAPI placement events safe for "
                + "ForgeDirection.UNKNOWN (GuideNH virtual multiblocks)");
        return writer.toByteArray();
    }
}
