package se.kth.jdbl.debloater;

import org.objectweb.asm.*;
import se.kth.jdbl.instrumenter.ClassCoverageCV;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class DebloaterClassFileTransformer implements ClassFileTransformer {

    private boolean shouldIgnore;
    private int nbMethodsRemoved;

    public int getNbMethodsRemoved() {
        return nbMethodsRemoved;
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        // For our purposes, skip java, sun and jdk internal methods
        if (className.startsWith("java/") || className.startsWith("sun/") || className.startsWith("jdk/")) {
            return null;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        if (shouldIgnore(cr)) {
            return classfileBuffer;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        try {
            ClassVisitor cv = new MethodDebloaterClassVisitor(cw);
            cr.accept(cv, 0);
            nbMethodsRemoved += ((MethodDebloaterClassVisitor) cv).getNbMethodsRemoved();
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Do not visit interfaces, enums and fields.
     * They were not instrumented so we should not remove them.
     *
     * @param cr
     * @return
     */
    private boolean shouldIgnore(ClassReader cr) {
        shouldIgnore = false;
        cr.accept(new ClassVisitor(Opcodes.ASM7) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                if (!((access & Opcodes.ACC_INTERFACE) == 0 && (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_ANNOTATION) == 0))
                    shouldIgnore = true; //o nly bother with classesUsed
            }
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (name.equals(ClassCoverageCV.CLASS_COVERAGE_FIELD))
                    shouldIgnore = true; // if the field is already there, then we instrumented it statically
                return super.visitField(access, name, desc, signature, value);
            }
        }, ClassReader.SKIP_CODE);
        return shouldIgnore;
    }
}
