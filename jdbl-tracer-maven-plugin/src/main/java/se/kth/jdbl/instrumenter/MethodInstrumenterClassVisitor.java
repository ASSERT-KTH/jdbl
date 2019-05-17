package se.kth.jdbl.instrumenter;

import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.HashSet;

public class MethodInstrumenterClassVisitor extends ClassVisitor {

    private String classKey;
    private String cName;
    private boolean fixLdcClass = false;
    private boolean isClass = false;
    private HashMap<String, String> keyToMethod = new HashMap<>();
    private HashSet<String> methods = new HashSet<>();

    /**
     * Constructor.
     *
     * @param cv the ClassVisitor to pass.
     */
    public MethodInstrumenterClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
    }

    /**
     * Visit the header of the class. This method visits only classesUsed (skips Enums and Interfaces).
     */
    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        isClass = (access & Opcodes.ACC_INTERFACE) == 0 && (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_ANNOTATION) == 0;
        this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
        this.cName = name;
        classKey = "__instHit" + cName.replace("/", "_");
    }

    /**
     * Visits a method of the class. This method <i>must</i> return a new MethodVisitor
     * instance (or {@literal null}) each time it is called, i.e., it should not return a previously
     * returned visitor.
     */
    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        final String key = "__instCounter_" + name.replace("<", "").replace(">", "") + methods.size();
        keyToMethod.put(key, name + desc);
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        return new MethodVisitor(Opcodes.ASM7, mv) {
            @Override
            public void visitCode() {
                if (isClass) {
                    methods.add(key);
                    // At method entry, check and see if we have locally cached that this method has been hit. If not, flag it.
                    super.visitCode();
                    Label ok = new Label();
                    super.visitFieldInsn(Opcodes.GETSTATIC, cName, key, "Z");
                    super.visitJumpInsn(Opcodes.IFNE, ok);
                    super.visitInsn(Opcodes.ICONST_1);
                    super.visitFieldInsn(Opcodes.PUTSTATIC, cName, key, "Z");
                    /*
                     * Make sure that we noticed that this class was hit. Could be done only once and placed in <clinit> instead,
                     * but I'm keeping it simple (we would have to make sure that each class
                     * already has a <clinit>, and add this code to it).
                     */
                    super.visitFieldInsn(Opcodes.GETSTATIC, cName, classKey, "Z");
                    super.visitJumpInsn(Opcodes.IFNE, ok);
                    super.visitInsn(Opcodes.ICONST_1);
                    super.visitFieldInsn(Opcodes.PUTSTATIC, cName, classKey, "Z");
                    if (fixLdcClass) {
                        super.visitLdcInsn(cName.replace("/", "."));
                        super.visitInsn(Opcodes.ICONST_0);
                        super.visitLdcInsn(cName.replace("/", "."));
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                    } else
                        super.visitLdcInsn(Type.getObjectType(cName));
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MethodInstrumenterLogger.class), "classHit", "(Ljava/lang/Class;)V", false);
                    super.visitLabel(ok);
                    super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                }
            }
        };
    }


    /**
     * Instruments the classes and methods to locally cache their hit state.
     * Updates the {@link MethodInstrumenterLogger} to record their state.
     */
    @Override
    public void visitEnd() {
        if (isClass) {
            for (String s : keyToMethod.keySet()) {
                // Add a field for every method to locally cache its hit state
                super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, s, "Z", null, 0);
            }
            // Add a field for the class itself to locally cache its hit state
            super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, classKey, "Z", null, 0);
            // Generate a method to collect all local method hit-state
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "__dumpMethodsHit", "()V", null, null);
            mv.visitCode();
            for (String key : keyToMethod.keySet()) {
                Label ok = new Label();
                mv.visitFieldInsn(Opcodes.GETSTATIC, cName, key, "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, ok);
                mv.visitLdcInsn(cName + "." + keyToMethod.get(key)); // Get the fully qualified name of this method
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MethodInstrumenterLogger.class), "methodHit", "(Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, cName, key, "Z"); // Reset local state
                mv.visitLabel(ok);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, cName, classKey, "Z"); // Reset local state

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}
