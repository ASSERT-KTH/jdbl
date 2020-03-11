package se.kth.castor.jdbl.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class JarUtils {

   private static final Logger LOGGER = LogManager.getLogger(JarUtils.class.getName());

   /**
    * Size of the buffer to read/write data.
    */
   private static final int BUFFER_SIZE = 16384;

   private JarUtils() {
   }

   /**
    * Decompress all jar files located in a given directory.
    */
   public static void decompressJars(String outputDirectory) {
      File files = new File(outputDirectory);
      for (File f : Objects.requireNonNull(files.listFiles())) {
         if (f.getName().endsWith(".jar")) {
            LOGGER.info("Decompressing:" + f.getName());
            try {
               JarUtils.decompressJarFile(f.getAbsolutePath(), outputDirectory);
               // delete the original dependency jar file
               Files.delete(f.toPath());
            } catch (IOException e) {
               LOGGER.error(e);
            }
         }
      }
   }

   /**
    * Decompress a jar file in a path to a directory (will be created if it doesn't exists).
    */
   private static void decompressJarFile(String jarFilePath, String destDirectory) throws IOException {
      File destDir = new File(destDirectory);
      if (!destDir.exists()) {
         destDir.mkdir();
      }
      try (JarInputStream jarIn = new JarInputStream(new FileInputStream(jarFilePath))) {
         JarEntry entry = jarIn.getNextJarEntry();
         // iterates over entries in the jar file
         while (entry != null) {
            String filePath = destDirectory + "/" + entry.getName();
            if (!entry.isDirectory()) {
               new File(filePath).getParentFile().mkdirs();
               // if the entry is a file, extracts it
               extractFile(jarIn, filePath);
            }/* else {
                   System.out.println("New dir: " + filePath);
                   // if the entry is a directory, make the directory
                   File dir = new File(filePath);
                   dir.mkdir();
                   System.out.println(dir.canWrite());
               }*/
            jarIn.closeEntry();
            entry = jarIn.getNextJarEntry();
         }
      }
   }

   /**
    * Extract an entry file.
    */
   private static void extractFile(JarInputStream jarIn, String filePath) throws IOException {
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
         byte[] bytesIn = new byte[BUFFER_SIZE];
         int read = 0;
         while ((read = jarIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
         }
      }
   }
}
