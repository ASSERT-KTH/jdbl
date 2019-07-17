package se.kth.jdbl.instrumenter;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class InstrumenterClassFileTransformer implements ClassFileTransformer {

    private boolean shouldIgnore;

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer)
            throws IllegalClassFormatException {

        // For our purposes, skip java, sun and jdk internal methods
        if (className.startsWith("java/") || className.startsWith("sun/") || className.startsWith("jdk/"))
            return null;

        ClassReader cr = new ClassReader(classfileBuffer);

        if (shouldIgnore(cr)) {
            // Do not remove interfaces, enums and fields.
//            UsageRecorder.addUsedClass(cr.getClassName().replace("/", "."));
            return classfileBuffer;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        try {
            ClassVisitor cv = new MethodInstrumenterClassVisitor(cw);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Ignore interfaces, enums and fields.
     *
     * @param cr
     * @return
     */
    private boolean shouldIgnore(ClassReader cr) {
        shouldIgnore = false;
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                if (!((access & Opcodes.ACC_INTERFACE) == 0 && (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_ANNOTATION) == 0)) {
                    shouldIgnore = true; //only bother with classesUsed
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (name.equals(ClassCoverageCV.CLASS_COVERAGE_FIELD))
                    shouldIgnore = true; //if the field is already there, then we instrumented it statically
                return super.visitField(access, name, desc, signature, value);
            }
        }, ClassReader.SKIP_CODE);
        return shouldIgnore;
    }
}
