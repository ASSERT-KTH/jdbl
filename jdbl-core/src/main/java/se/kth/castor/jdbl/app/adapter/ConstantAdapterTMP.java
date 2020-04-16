package se.kth.castor.jdbl.app.adapter;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ConstantAdapterTMP extends ClassVisitor implements Opcodes
{

   private String baseDir;

   public ConstantAdapterTMP(final ClassVisitor cv, String baseDir)
   {
      super(ASM7, cv);
      this.baseDir = baseDir;
   }

   public void visitAttribute(final Attribute attribute)
   {
      if (cv != null) {
         cv.visitAttribute(attribute);
      }
   }

   public void visitCode()
   {

   }

   public static byte[] transform(byte[] origClassData)
   {
      ClassReader cr = new ClassReader(origClassData);
      final ClassWriter cw = new ClassWriter(cr, Opcodes.ASM5);

      // add the static final fields
      cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "FIRST", "LExample;", null, null).visitEnd();
      cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "SECOND", "LExample;", null, null).visitEnd();

      // wrap the ClassWriter with a ClassVisitor that adds the static block to
      // initialize the above fields
      ClassVisitor cv = new ClassVisitor(ASM5, cw)
      {
         boolean visitedStaticBlock = false;

         class StaticBlockMethodVisitor extends MethodVisitor
         {
            StaticBlockMethodVisitor(MethodVisitor mv)
            {
               super(ASM5, mv);
            }

            public void visitCode()
            {
               super.visitCode();

               // here we do what the static block in the java code
               // above does i.e. initialize the FIRST and SECOND
               // fields

               // create first instance
               super.visitTypeInsn(NEW, "A");
               super.visitInsn(DUP);
               super.visitInsn(ICONST_1); // pass argument 1 to constructor
               super.visitMethodInsn(INVOKESPECIAL, "Example", "<init>", "(I)V");
               // store it in the field
               super.visitFieldInsn(PUTSTATIC, "Example", "FIRST", "LExample;");

               // NOTE: remember not to put a RETURN instruction
               // here, since execution should continue
            }

            public void visitMaxs(int maxStack, int maxLocals)
            {
               // The values 3 and 0 come from the fact that our instance
               // creation uses 3 stack slots to construct the instances
               // above and 0 local variables.
               final int ourMaxStack = 3;
               final int ourMaxLocals = 0;

               // now, instead of just passing original or our own
               // visitMaxs numbers to super, we instead calculate
               // the maximum values for both.
               super.visitMaxs(Math.max(ourMaxStack, maxStack), Math.max(ourMaxLocals, maxLocals));
            }
         }

         public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
         {
            if (cv == null) {
               return null;
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("<clinit>".equals(name) && !visitedStaticBlock) {
               visitedStaticBlock = true;
               return new StaticBlockMethodVisitor(mv);
            } else {
               return mv;
            }
         }

         public void visitEnd()
         {
            // All methods visited. If static block was not
            // encountered, add a new one.
            if (!visitedStaticBlock) {
               // Create an empty static block and let our method
               // visitor modify it the same way it modifies an
               // existing static block
               MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
               mv = new StaticBlockMethodVisitor(mv);
               mv.visitCode();
               mv.visitInsn(RETURN);
               mv.visitMaxs(0, 0);
               mv.visitEnd();
            }
            super.visitEnd();
         }
      };

      // feed the original class to the wrapped ClassVisitor
      cr.accept(cv, 0);

      // produce the modified class
      byte[] newClassData = cw.toByteArray();
      return newClassData;
   }
}
