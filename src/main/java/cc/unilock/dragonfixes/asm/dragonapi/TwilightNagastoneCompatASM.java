package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * DragonAPI looks up TFBlocks.nagastone, renamed oldNagastone by GTNH's
 * Twilight Forest fork when the new multi-block Naga Stone variants arrived.
 * Preserve DragonAPI's legacy block identity and reflection-based integration.
 */
public final class TwilightNagastoneCompatASM implements IClassTransformer {
    private static final String TARGET =
            "Reika.DragonAPI.ModInteract.ItemHandlers.TwilightForestHandler$BlockEntry";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)
                || Launch.classLoader.getResource(
                        "twilightforest/block/BlockTFNagastone2.class") == null) {
            return bytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changed = 0;
        for (MethodNode method : node.methods) {
            if (!"<clinit>".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode
                        && "nagastone".equals(((LdcInsnNode) insn).cst)) {
                    ((LdcInsnNode) insn).cst = "oldNagastone";
                    changed++;
                }
            }
        }
        if (changed != 1) {
            DragonFixes.LOGGER.warn("Expected one legacy Naga Stone field name, "
                    + "found {}; leaving integration unchanged", changed);
            return bytes;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        DragonFixes.LOGGER.info("Mapped DragonAPI Naga Stone lookup to GTNH Twilight Forest's oldNagastone");
        return writer.toByteArray();
    }
}
