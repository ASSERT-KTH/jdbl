package se.kth.castor.jdbl.app.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import se.kth.castor.jdbl.app.adapter.CustomClassReader;

public class JDblFileUtils
{
   /**
    * Counts the number of classes removed.
    */
   private int nbClassesRemoved;

   /**
    * The build outputDirectory.
    */
   private String outputDirectory;

   /**
    * Exclusion list of classes and package names that should not be removed.
    */
   private Set<String> exclusionSet;

   /**
    * Set of the binary names of classesUsed traced.
    */
   private Set<String> classesUsed;

   /**
    * Report path
    */
   private File reportFile;

   /**
    * Class logger.
    */
   private static final Logger LOGGER = LogManager.getLogger(JDblFileUtils.class.getName());
   private List<String> classpath;

   public JDblFileUtils(String outputDirectory, Set<String> exclusionSet, Set<String> classesUsed, File reportFile, List<String> classpath)
   {
      this.classpath = classpath;
      this.nbClassesRemoved = 0;
      this.outputDirectory = outputDirectory;
      this.exclusionSet = exclusionSet;
      this.classesUsed = classesUsed;
      this.reportFile = reportFile;
   }

   /**
    * Feed the list of non-removable classes.
    *
    * @param pathToFile The exclusion list file which contains the list of classes that will not be deleted.
    */
   public void setExclusionList(String pathToFile)
   {
      Path path = Paths.get(pathToFile);
      try (Stream<String> lines = Files.lines(path)) {
         lines.forEach(s -> this.exclusionSet.add(s.replaceAll("/", ".")));
      } catch (IOException e) {
         LOGGER.error(e);
      }
   }

   /**
    * Recursively remove unused classes in a directory.
    *
    * @param currentPath the start file path to delete.
    */
   public void deleteUnusedClasses(String currentPath) throws IOException
   {
      URLClassLoader urlClassLoader = null;
      if (this.classpath != null) {
         URL[] urls = new URL[this.classpath.size()];
         for (int i = 0; i < this.classpath.size(); i++) {
            urls[i] = new File(this.classpath.get(i)).toURL();
         }
         urlClassLoader = new URLClassLoader(urls, null);
      }
      File file = new File(currentPath);
      File[] list = file.listFiles();
      assert list != null;
      for (File classFile : list) {
         if (classFile.isDirectory()) {
            // recursive call for directories
            deleteUnusedClasses(classFile.getAbsolutePath());
         } else if (classFile.getName().endsWith(".class")) {
            String classFilePath = classFile.getAbsolutePath();
            String currentClassName = getBinaryNameOfTestFile(classFilePath);
            String fileType = "class";
            if (currentClassName == null) {
               continue;
            }
            try {
               if (urlClassLoader != null) {
                  Class<?> aClass = urlClassLoader.loadClass(currentClassName);
                  if (aClass.isAnnotation()) {
                     fileType = "annotation";
                     exclusionSet.add(currentClassName);
                  } else if (aClass.isInterface()) {
                     fileType = "interface";
                     exclusionSet.add(currentClassName);
                  } else if (aClass.isEnum()) {
                     fileType = "enum";
                     exclusionSet.add(currentClassName);
                  } else {
                     try {
                        if (Modifier.isPrivate(aClass.getConstructor().getModifiers())) {
                           fileType = "constant";
                           exclusionSet.add(currentClassName);
                        }
                     } catch (Throwable e) {
                        // ignore
                     }
                     try {
                        boolean allStatic = true;
                        for (Field field : aClass.getFields()) {
                           allStatic = allStatic && Modifier.isStatic(field.getModifiers());
                        }
                        for (Method method : aClass.getMethods()) {
                           allStatic = allStatic && Modifier.isStatic(method.getModifiers());
                        }
                        if (allStatic) {
                           fileType = "constant";
                           exclusionSet.add(currentClassName);
                        }
                     } catch (Throwable e) {
                        // ignore
                     }
                     try {
                        if (Modifier.isStatic(aClass.getField("INSTANCE").getModifiers())) {
                           fileType = "constant";
                           exclusionSet.add(currentClassName);
                        }
                     } catch (Throwable e) {
                        // ignore
                     }
                  }
               }
            } catch (Throwable e) {
               // ignore
               fileType = "unknown";
            }
            // do not remove interfaces
            CustomClassReader ccr = new CustomClassReader(new FileInputStream(classFilePath));

            if (!classesUsed.contains(currentClassName) &&
               isRemovable(currentClassName.replace("/", ".")) &&
               !exclusionSet.contains(currentClassName) &&
               !ccr.isInterface() &&
               !ccr.isException()) {
               // get the current directory
               File parent = new File(classFile.getParent());
               // remove the file
               LOGGER.info("Removed class: " + currentClassName);
               // write report
               FileUtils.writeStringToFile(this.reportFile, "BloatedClass," + currentClassName + "," + fileType +
                  "\n", StandardCharsets.UTF_8, true);
               Files.delete(classFile.toPath());
               nbClassesRemoved++;
               // remove the parent folder if is empty
               while (parent.isDirectory() && Objects.requireNonNull(parent.listFiles()).length == 0) {
                  deleteDirectory(parent);
                  parent = parent.getParentFile();
               }
            } else {
               // write report
               FileUtils.writeStringToFile(this.reportFile, "UsedClass," + currentClassName + "," + fileType +
                  "\n", StandardCharsets.UTF_8, true);
            }
         }
      }
   }

   private String getBinaryNameOfTestFile(final String classFilePath)
   {
      return classFilePath
         .replaceAll("/", ".")
         .substring(outputDirectory.length() + 1, classFilePath.length() - 6);
   }

   public int getNbClassesRemoved()
   {
      return nbClassesRemoved;
   }

   private boolean isRemovable(String className) throws IOException
   {
      //
      //        System.out.println("The classname: " + className);
      //
      //        boolean result;
      //
      //        BufferedReader reader;
      //        try {
      //            reader = new BufferedReader(new FileReader(
      //  "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/clitools/loaded-classes"));
      //            String line = reader.readLine();
      //            while (line != null) {
      //                if (line.equals(className)) {
      //                    return false;
      //                }
      //                line = reader.readLine();
      //            }
      //            reader.close();
      //        } catch (IOException e) {
      //            e.printStackTrace();
      //        }

      return true;

      //        ClassReader cr = new ClassReader(new FileInputStream(new File(pathToClass)));
      //        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      //        ClassAdapter cv = new ClassAdapter(cw, outputDirectory);
      //        cr.accept(cv, 0);
      //
      //        return cv.isRemovable;
   }

   private void deleteDirectory(final File directory) throws IOException
   {
      if (!directory.exists()) {
         return;
      }
      if (!isSymlink(directory)) {
         cleanDirectory(directory);
      }
      if (!directory.delete()) {
         final String message = "Unable to delete directory " + directory + ".";
         throw new IOException(message);
      }
   }

   private boolean isSymlink(final File file)
   {
      if (file == null) {
         throw new NullPointerException("File must not be null");
      }
      return Files.isSymbolicLink(file.toPath());
   }

   private void cleanDirectory(final File directory) throws IOException
   {
      final File[] files = verifiedListFiles(directory);
      IOException exception = null;
      for (final File file : files) {
         try {
            forceDelete(file);
         } catch (final IOException ioe) {
            exception = ioe;
         }
      }
      if (null != exception) {
         throw exception;
      }
   }

   private File[] verifiedListFiles(final File directory) throws IOException
   {
      if (!directory.exists()) {
         final String message = directory + " does not exist";
         throw new IllegalArgumentException(message);
      }
      if (!directory.isDirectory()) {
         final String message = directory + " is not a directory";
         throw new IllegalArgumentException(message);
      }
      final File[] files = directory.listFiles();
      if (files == null) {  // null if security restricted
         throw new IOException("Failed to list contents of " + directory);
      }
      return files;
   }

   private void forceDelete(final File file) throws IOException
   {
      if (file.isDirectory()) {
         deleteDirectory(file);
      } else {
         final boolean filePresent = file.exists();
         if (!file.delete()) {
            if (!filePresent) {
               throw new FileNotFoundException("File does not exist: " + file);
            }
            final String message = "Unable to delete file: " + file;
            throw new IOException(message);
         }
      }
   }
}
