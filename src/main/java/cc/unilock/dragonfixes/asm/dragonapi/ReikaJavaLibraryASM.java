package cc.unilock.dragonfixes.asm.dragonapi;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
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
            return basicClass;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
