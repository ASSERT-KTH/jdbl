package se.kth.jdbl.adapter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ClassAdapter extends ClassVisitor implements Opcodes {

    public boolean isRemovable;

    private String baseDir;

    public ClassAdapter(final ClassVisitor cv, String baseDir) {
        super(ASM5, cv);
        this.baseDir = baseDir;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        isRemovable = !(((access & Opcodes.ACC_INTERFACE) != 0) ||
                ((access & Opcodes.ACC_ENUM) != 0) ||
                ((access & Opcodes.ACC_ANNOTATION) != 0) ||
                ((access & Opcodes.ACC_FINAL) != 0) ||
//                (name.matches(".*[$]\\d+")) || // annonymous classes
                (name.contains("$")) ||
                extendsThrowable(superName));
    }

    private boolean extendsThrowable(String className) {
        ClassReader cr = null;
        try {
            if (className.startsWith("java/") || className.startsWith("sun/")) {

                if (className.endsWith("Exception") || className.endsWith("Error")) {
                    return true;
                }
                return false;
            } else {
                cr = new ClassReader(new FileInputStream(new File(baseDir + "/" + className + ".class")));
            }
        } catch (IOException e) {
            return true;
        }
        String superName = cr.getSuperName();
        if (superName != null && !superName.equals("java/lang/Object")) {
            String superClass = superName.replace('.', '/');
            return extendsThrowable(superClass);
        }
        return false;
    }
}

