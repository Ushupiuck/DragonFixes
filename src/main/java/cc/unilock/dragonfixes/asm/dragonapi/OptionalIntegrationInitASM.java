package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guard only the two unconditional optional-helper initializations in DragonAPI postload. */
public final class OptionalIntegrationInitASM implements IClassTransformer {
    private static final String TARGET = "Reika.DragonAPI.DragonAPIInit";
    private static final String HELPER_PREFIX = "Reika/DragonAPI/ModInteract/DeepInteract/";
    private static final String BRIDGE = "cc/unilock/dragonfixes/compat/OptionalIntegrationInit";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET.equals(transformedName)) return basicClass;
        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!"postload".equals(method.name)
                    || !"(Lcpw/mods/fml/common/event/FMLPostInitializationEvent;)V".equals(method.desc)) continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() != Opcodes.INVOKESTATIC
                        || !"Reika/DragonAPI/Libraries/Java/ReikaJavaLibrary".equals(call.owner)
                        || !"initClass".equals(call.name)
                        || !"(Ljava/lang/Class;Z)V".equals(call.desc)) continue;
                AbstractInsnNode flag = previousInstruction(call);
                if (flag == null || flag.getOpcode() != Opcodes.ICONST_1) continue;
                AbstractInsnNode literal = previousInstruction(flag);
                if (!(literal instanceof LdcInsnNode) || !(((LdcInsnNode) literal).cst instanceof Type)) continue;
                Type type = (Type) ((LdcInsnNode) literal).cst;
                if (type.getSort() != Type.OBJECT) continue;
                String helper = type.getInternalName();
                String modId;
                if ((HELPER_PREFIX + "FrameBlacklist").equals(helper)) modId = "framez";
                else if ((HELPER_PREFIX + "ReikaMystcraftHelper").equals(helper)) modId = "Mystcraft";
                else continue;
                // Same stack depth and straight-line control flow. Do not resolve
                // the optional class literal before checking the loaded mod.
                method.instructions.set(literal, new LdcInsnNode(type.getClassName()));
                method.instructions.set(flag, new LdcInsnNode(modId));
                method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE,
                        "initializeIfLoaded", "(Ljava/lang/String;Ljava/lang/String;)V", false));
                count++;
            }
        }
        if (count != 2) {
            LogManager.getLogger("dragonfixes").warn("Matched {} of 2 DragonAPI optional integration initialization calls", count);
        }
        if (count == 0) return basicClass;
        LogManager.getLogger("dragonfixes").info("Guarded {} DragonAPI optional integration initialization calls", count);
        ClassWriter writer = new ClassWriter(reader, 0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static AbstractInsnNode previousInstruction(AbstractInsnNode node) {
        AbstractInsnNode prev = node.getPrevious();
        while (prev != null && prev.getOpcode() < 0) prev = prev.getPrevious();
        return prev;
    }
}
