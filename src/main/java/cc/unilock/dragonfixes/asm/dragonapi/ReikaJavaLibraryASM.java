package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ReikaJavaLibraryASM implements IClassTransformer {
    private static final String TARGET_CLASS = "Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary";
    private static final String TARGET_METHOD = "getMaximumRecursiveDepth";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || basicClass.length == 0 || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);

        boolean patched = false;
        boolean optionalGuardPatched = false;
        // The API's post-init blindly initializes both helpers even when their
        // optional dependencies are absent. Skip initialization, not the error log.
        for (MethodNode method : classNode.methods) {
            if ("initClass".equals(method.name)
                    && "(Ljava/lang/Class;Z)V".equals(method.desc)
                    && (method.access & Opcodes.ACC_STATIC) != 0) {
                InsnList guard = new InsnList();
                LabelNode original = new LabelNode();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
                guard.add(new JumpInsnNode(Opcodes.IFNULL, original));
                appendOptionalGuard(guard,
                        "Reika.DragonAPI.ModInteract.DeepInteract.FrameBlacklist", "framez", original);
                appendOptionalGuard(guard,
                        "Reika.DragonAPI.ModInteract.DeepInteract.ReikaMystcraftHelper", "Mystcraft", original);
                guard.add(original);
                method.instructions.insert(guard);
                optionalGuardPatched = true;
                break;
            }
        }
        if (!optionalGuardPatched) {
            LogManager.getLogger("DragonFixes").warn("Could not find DragonAPI initClass(Class, boolean) for optional integration guard");
        }
        for (MethodNode methodNode : classNode.methods) {
            if (TARGET_METHOD.equals(methodNode.name) && "()I".equals(methodNode.desc)) {
                // The old body may contain labels referenced by exception/debug metadata.
                // Remove them with the instructions to leave valid, self-contained bytecode.
                methodNode.instructions.clear();
                methodNode.tryCatchBlocks.clear();
                if (methodNode.localVariables != null) {
                    methodNode.localVariables.clear();
                }
                if (methodNode.visibleLocalVariableAnnotations != null) {
                    methodNode.visibleLocalVariableAnnotations.clear();
                }
                if (methodNode.invisibleLocalVariableAnnotations != null) {
                    methodNode.invisibleLocalVariableAnnotations.clear();
                }

                methodNode.instructions.add(new LdcInsnNode(1000));
                methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
                patched = true;
                break;
            }
        }

        if (!patched) {
            LogManager.getLogger("DragonFixes").warn("Could not find {}()I in {}; leaving class unchanged",
                    TARGET_METHOD, TARGET_CLASS);
            if (!optionalGuardPatched) return basicClass;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void appendOptionalGuard(InsnList code, String className, String modId, LabelNode original) {
        LabelNode next = new LabelNode();
        code.add(new LdcInsnNode(className));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName",
                "()Ljava/lang/String;", false));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals",
                "(Ljava/lang/Object;)Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, next));
        code.add(new LdcInsnNode(modId));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/Loader",
                "isModLoaded", "(Ljava/lang/String;)Z", false));
        code.add(new JumpInsnNode(Opcodes.IFNE, original));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(next);
    }
}
