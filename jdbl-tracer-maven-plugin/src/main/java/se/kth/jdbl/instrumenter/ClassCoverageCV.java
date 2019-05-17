package se.kth.jdbl.instrumenter;


import org.objectweb.asm.*;

public class ClassCoverageCV extends ClassVisitor implements Opcodes {

    public static final String CLASS_COVERAGE_FIELD = "__ClassCovered";

    private String className;
    private boolean isClass;
    private boolean fixLdcClass;

    public ClassCoverageCV(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
        this.isClass = (access & Opcodes.ACC_INTERFACE) == 0 && (access & Opcodes.ACC_ENUM) == 0;
        System.out.println("Visited class name: " + name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM7, mv) {
            @Override
            public void visitCode() {
                if (isClass) {
                    /*
                     * Make sure that we noticed that this class was hit.
                     */
                    Label ok = new Label();
                    super.visitFieldInsn(Opcodes.GETSTATIC, className, CLASS_COVERAGE_FIELD, "Z");
                    super.visitJumpInsn(Opcodes.IFNE, ok);
                    super.visitInsn(Opcodes.ICONST_1);
                    super.visitFieldInsn(Opcodes.PUTSTATIC, className, CLASS_COVERAGE_FIELD, "Z");
                    if (fixLdcClass) {
                        super.visitLdcInsn(className.replace("/", "."));
                        super.visitInsn(Opcodes.ICONST_0);
                        super.visitLdcInsn(className.replace("/", "."));
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                    } else {
                        super.visitLdcInsn(Type.getObjectType(className));
                    }
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(UsageLogger.class), "classHit", "(Ljava/lang/Class;)V", false);
                    super.visitLabel(ok);
                    super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                }
            }
        };
    }

    @Override
    public void visitEnd() {
        if (isClass)
            super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, CLASS_COVERAGE_FIELD, "Z", null, false);
        super.visitEnd();
    }
}
