package se.kth.castor.jdbl.debloat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EntryPointMethodDebloat extends AbstractMethodDebloat {

   //--------------------------------/
   //-------- CONSTRUCTOR/S --------/
   //------------------------------/

   public EntryPointMethodDebloat(String outputDirectory, Map<String, Set<String>> usageAnalysis) {
      super(outputDirectory, usageAnalysis);
   }

   //--------------------------------/
   //------ PRIVATE METHOD/S -------/
   //------------------------------/

   @Override
   public void removeMethod(String clazz, Set<String> usedMethods) throws IOException {
      FileInputStream in = new FileInputStream(new File(outputDirectory + "/" + clazz + ".class"));
      ClassReader cr = new ClassReader(in);
      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
         @Override
         public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

            if (usedMethods.contains(name + desc)) {
               System.out.println("Removed unused method: " + name + desc + " in class ==> " + clazz);
               return new MethodExceptionThrower(mv);
               // return null;
            }
            return mv;
            // return super.visitMethod(access, name, desc, signature, exceptions);
         }
      };
      cr.accept(cv, ClassReader.SKIP_DEBUG);

      byte[] code = cw.toByteArray();
      OutputStream fos = new FileOutputStream(outputDirectory + "/" + clazz.replace(".", "/") + ".class");
      fos.write(code);
      fos.close();
   }
}
