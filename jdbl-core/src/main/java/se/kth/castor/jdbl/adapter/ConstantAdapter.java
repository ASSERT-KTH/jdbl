package se.kth.castor.jdbl.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ConstantAdapter
{
    private ClassReader reader;
    private ClassWriter writer;

    public ConstantAdapter(InputStream classInputStream)
    {
        try {
            reader = new ClassReader(classInputStream);
            writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        } catch (IOException ex) {
            Logger.getLogger(ConstantAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ConstantAdapter(String className)
    {
        try {
            reader = new ClassReader(className);
            writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        } catch (IOException ex) {
            Logger.getLogger(ConstantAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException
    {
        ConstantAdapter ccw = new ConstantAdapter(
            new FileInputStream(new File("./jdbl-maven-plugin/src/it/dummy-project/target/classes/calc/StaticCl.class")));
        byte[] result = ccw.addField();
        FileUtils.writeByteArrayToFile(
            new File("/home/cesarsv/Documents/codes/github/jdbl/jdbl-core/StaticCl.class"), result);
    }

    public byte[] addField()
    {
        final AddFieldAdapter addFieldAdapter = new AddFieldAdapter("aNewBooleanField", Opcodes.ACC_PUBLIC, writer);
        reader.accept(addFieldAdapter, 0);
        return writer.toByteArray();
    }

    public static class AddFieldAdapter extends ClassVisitor
    {
        private String currClassName;
        int access;

        private final Map<String[], Object> constants = new HashMap<>();

        public AddFieldAdapter(String fieldName, int access, ClassVisitor cv)
        {
            super(Opcodes.ASM8, cv);
            this.cv = cv;
            this.access = access;
        }

        @Override
        public void visit(int version, int acc, String name, String sig, String superName, String[] ifs)
        {
            currClassName = name;
            super.visit(version, acc, name, sig, superName, ifs);
        }

        // @Override
        // public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        // {
        //    System.out.println("access: " + access);
        //    System.out.println("name: " + name);
        //    System.out.println("desc: " + desc);
        //    System.out.println("signature: " + signature);
        //    System.out.println("value: " + value);
        //    return cv.visitField(access, name, desc, signature, value);
        // }

        @Override
        public FieldVisitor visitField(int acc, String name, String desc, String sig, Object value)
        {

            if (value != null && (acc & Opcodes.ACC_STATIC) != 0) {
                constants.put(new String[]{currClassName, name, desc}, value);
                // System.out.println("visiting constant: " + constants);
            }
            return super.visitField(acc, name, desc, sig, null);
        }

        // @Override
        // public void visitEnd()
        // {
        //    if (!isFieldPresent) {
        //       FieldVisitor fv = cv.visitField(access, fieldName, Type.BOOLEAN_TYPE.toString(), null, null);
        //       if (fv != null) {
        //          fv.visitEnd();
        //       }
        //    }
        //    // cv.visitEnd();
        // }

        // @Override
        // public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        // {
        //    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        //    if ("<clinit>".equals(name) && !visitedStaticBlock) {
        //       visitedStaticBlock = true;
        //       return new StaticBlockMethodVisitor(mv);
        //    } else {
        //       return mv;
        //    }
        // }

        @Override
        public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex)
        {
            MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
            if (name.equals("<clinit>")) {
                mv = new MethodVisitor(Opcodes.ASM8, mv)
                {
                    @Override
                    public void visitCode()
                    {
                        super.visitCode();
                        injectFieldInit(this, constants);
                        constants.clear();
                    }
                };
            }
            return mv;
        }

        @Override
        public void visitEnd()
        {
            if (!constants.isEmpty()) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();
                injectFieldInit(mv, constants);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(-1, -1);
                mv.visitEnd();
            }
            super.visitEnd();
        }

        // public void visitEnd()
        // {
        //    // All methods visited. If static block was not
        //    // encountered, add a new one.
        //    if (!visitedStaticBlock) {
        //       // Create an empty static block and let our method
        //       // visitor modify it the same way it modifies an
        //       // existing static block
        //       MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        //       mv = new StaticBlockMethodVisitor(mv);
        //       mv.visitCode();
        //       mv.visitInsn(Opcodes.RETURN);
        //       mv.visitMaxs(0, 0);
        //       mv.visitEnd();
        //    }
        //    super.visitEnd();
        // }
    }

    private static void injectFieldInit(MethodVisitor target, Map<String[], Object> constants)
    {
        for (Map.Entry<String[], Object> e : constants.entrySet()) {
            target.visitLdcInsn(e.getValue());
            String[] field = e.getKey();
            target.visitFieldInsn(Opcodes.PUTSTATIC, field[0], field[1], field[2]);
        }
    }

    public static class StaticBlockMethodVisitor extends MethodVisitor
    {

        StaticBlockMethodVisitor(MethodVisitor mv)
        {
            super(Opcodes.ASM8, mv);
        }

        @Override
        public void visitCode()
        {
            super.visitCode();

            // here we do what the static block in the java code
            // above does i.e. initialize the FIRST and SECOND
            // fields

            // create first instance
            super.visitTypeInsn(Opcodes.NEW, "Example");
            super.visitInsn(Opcodes.DUP);
            super.visitInsn(Opcodes.ICONST_1); // pass argument 1 to constructor
            super.visitMethodInsn(Opcodes.INVOKESPECIAL, "Example", "<init>", "(I)V", false);
            // store it in the field
            // super.visitFieldInsn(Opcodes.PUTSTATIC, "Example", "FIRST", "LExample;");

            // NOTE: remember not to put a RETURN instruction
            // here, since execution should continue
        }


    }
}
