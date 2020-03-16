package se.kth.castor.jdbl.app.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class JarUtils
{
   private static final Logger LOGGER = LogManager.getLogger(JarUtils.class.getName());

   private static HashSet<DependencyFileMapper> dependencyFileMappers = new HashSet<>();
   private static DependencyFileMapper currentDependencyFileMapper;
   private static String currentJarName = "";

   /**
    * Size of the buffer to read/write data.
    */
   private static final int BUFFER_SIZE = 16384;

   private JarUtils()
   {
   }

   /**
    * Decompress all jar files located in a given directory.
    */
   public static void decompressJars(String outputDirectory)
   {
      File files = new File(outputDirectory);
      for (File f : Objects.requireNonNull(files.listFiles())) {
         if (f.getName().endsWith(".jar")) {
            LOGGER.info("Decompressing:" + f.getName() + "in " + outputDirectory);
            try {
               currentDependencyFileMapper = new DependencyFileMapper();
               currentJarName = f.getName();
               currentDependencyFileMapper.addDependencyJar(currentJarName);

               JarUtils.decompressJarFile(f.getAbsolutePath(), outputDirectory);

               dependencyFileMappers.add(currentDependencyFileMapper);
               cleanupTheLocalFields();

               // delete the original dependency jar file
               f.delete();
            } catch (IOException e) {
               LOGGER.error(e);
            }
         }
      }
   }

   /**
    * Decompress a jar file in a path to a directory (will be created if it doesn't exists).
    */
   public static void decompressJarFile(String jarFilePath, String destDirectory) throws IOException
   {
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
               extractFile(jarIn, filePath, destDirectory);
            }
            jarIn.closeEntry();
            entry = jarIn.getNextJarEntry();
         }
      }
   }

   public static HashSet<DependencyFileMapper> getDependencyFileMappers()
   {
      return dependencyFileMappers;
   }

   private static void cleanupTheLocalFields()
   {
      currentDependencyFileMapper = null;
      currentJarName = null;
   }

   /**
    * Extract an entry file.
    */
   private static void extractFile(JarInputStream jarIn, String filePath, String destDir) throws IOException
   {
      // add class to the currentDependencyFileMapper
      if (filePath.endsWith(".class")) {
         String classInDependency = filePath.replace(destDir + "/", "").replace("/", ".").replace(".class", "");
         currentDependencyFileMapper.addClassFileToDependencyJar(currentJarName, classInDependency);
      }
      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
         byte[] bytesIn = new byte[BUFFER_SIZE];
         int read = 0;
         while ((read = jarIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
         }
      }
   }

   /**
    * This class stores the dependencies and their classes (Useful to keep a record of classes that were debloated in
    * each dependency)
    */
   public static class DependencyFileMapper
   {
      /**
       * Map: dependency jar -> Set< path of the jar files inside the jar >
       */
      private HashMap<String, HashSet<String>> dependencyClassMap;

      public DependencyFileMapper()
      {
         dependencyClassMap = new HashMap<>();
      }

      public void addDependencyJar(String dependencyJar)
      {
         dependencyClassMap.put(dependencyJar, new HashSet<>());
      }

      public void addClassFileToDependencyJar(String dependencyJar, String classFile)
      {
         dependencyClassMap.get(dependencyJar).add(classFile);
      }

      public HashMap<String, HashSet<String>> getDependencyClassMap()
      {
         return dependencyClassMap;
      }
   }
}
