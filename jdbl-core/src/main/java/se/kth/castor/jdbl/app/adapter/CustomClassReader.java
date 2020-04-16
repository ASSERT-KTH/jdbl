package se.kth.castor.jdbl.app.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class CustomClassReader
{
   private ClassReader cr;

   public CustomClassReader(InputStream classInputStream)
   {
      try {
         cr = new ClassReader(classInputStream);
      } catch (IOException e) {
         Logger.getLogger(CustomClassReader.class.getName()).log(Level.SEVERE, null, e);
      }
   }

   public CustomClassReader(String className)
   {
      try {
         cr = new ClassReader(className);
      } catch (IOException e) {
         Logger.getLogger(CustomClassReader.class.getName()).log(Level.SEVERE, null, e);
      }
   }

   public boolean isInterface()
   {
      return ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0);
   }

   public boolean isException()
   {
      return (cr.getSuperName().endsWith("Exception"));
   }
}
