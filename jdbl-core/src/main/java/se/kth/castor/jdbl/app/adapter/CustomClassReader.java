package se.kth.castor.jdbl.app.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class CustomClassReader
{
   ClassReader reader;
   ClassWriter writer;

   public CustomClassReader(InputStream inputStream)
   {
      try {
         reader = new ClassReader(inputStream);
         writer = new ClassWriter(reader, 0);
      } catch (IOException e) {
         Logger.getLogger(CustomClassReader.class.getName()).log(Level.SEVERE, null, e);
      }
   }

   public CustomClassReader(String className)
   {
      try {
         reader = new ClassReader(className);
         writer = new ClassWriter(reader, 0);
      } catch (IOException e) {
         Logger.getLogger(CustomClassReader.class.getName()).log(Level.SEVERE, null, e);
      }
   }

   public boolean isInterface()
   {
      return ((reader.getAccess() & Opcodes.ACC_INTERFACE) != 0);
   }
}
