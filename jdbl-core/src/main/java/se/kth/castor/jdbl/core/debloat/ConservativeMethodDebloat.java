package se.kth.castor.jdbl.core.debloat;

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

public class ConservativeMethodDebloat extends AbstractMethodDebloat
{
   public ConservativeMethodDebloat(String outputDirectory, Map<String, Set<String>> usageAnalysis, File fileReport)
   {
      super(outputDirectory, usageAnalysis, fileReport);
   }

   @Override
   public void removeMethod(String clazz, Set<String> usedMethods) throws IOException
   {
      try {
         FileInputStream in = new FileInputStream(new File(outputDirectory + "/" + clazz + ".class"));
         ClassReader cr = new ClassReader(in);
         ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
         ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw)
         {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
               MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

               if (clazz.equals("calc/Calculator")) {
                  System.out.println("method name: " + name + ", " + "desc: " + desc + ", " + "signature: " + signature + ", " + "access: " + access);
                  System.out.println(usedMethods);
               }

               if (isContains(name, usedMethods)) {
                  //                        System.out.println("Removing unused method: " + name + desc + " in class: " + clazz);
                  return new MethodExceptionThrower(mv);
                  // return null;
               }
               return mv;
               // return super.visitMethod(access, name, desc, signature, exceptions);
            }
         };
         cr.accept(cv, ClassReader.SKIP_DEBUG);

         byte[] code = cw.toByteArray();
         try (OutputStream fos = new FileOutputStream(outputDirectory + "/" + clazz.replace(".", "/") + ".class")) {
            fos.write(code);
         }

      } catch (Exception e) {
      } // do nothing, just continue analyzing other classes
   }

   private boolean isContains(String methodName, Set<String> usedMethods)
   {
      if (methodName.equals("main")) {
         return true; // keep main classes
      }
      for (String usedMethod : usedMethods) {
         if (usedMethod.split("\\(")[0].equals(methodName)) {
            return true;
         }
      }
      return false;
   }
}
