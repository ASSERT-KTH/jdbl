package se.kth.castor.jdbl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import se.kth.castor.jdbl.adapter.CustomClassReader;
import se.kth.castor.jdbl.coverage.UsageStatusEnum;

public class MyFileUtils
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
    private String projectBaseDir;

    /**
     * Class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(MyFileUtils.class.getName());

    /**
     * The classpath of the classes
     */
    private List<String> classpath;

    public MyFileUtils(String outputDirectory, Set<String> exclusionSet, Set<String> classesUsed, String projectBaseDir,
        List<String> classpath)
    {
        this.classpath = classpath;
        this.nbClassesRemoved = 0;
        this.outputDirectory = outputDirectory;
        this.exclusionSet = exclusionSet;
        this.classesUsed = classesUsed;
        this.projectBaseDir = projectBaseDir;
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
     * @param currentPath the absolute path of the directory to be processed.
     */
    public void deleteUnusedClasses(String currentPath, String dirPath) throws IOException
    {
        URLClassLoader urlClassLoader = null;
        MyFileWriter myFileWriter = new MyFileWriter(projectBaseDir);
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
                // Recursive call for directories
                deleteUnusedClasses(classFile.getAbsolutePath(), dirPath);
            } else if (classFile.getName().endsWith(".class")) {
                String classFilePath = classFile.getAbsolutePath();
                String currentClassName = getBinaryNameOfClassFiles(classFilePath, dirPath);
                ClassFileType classFileType = getClassFileType(urlClassLoader, currentClassName, classFilePath);

                if (!classesUsed.contains(currentClassName)) {
                    // Do not remove the classes that are preserved based on our static analysis criteria
                    // if (classFileType.equals(ClassFileType.ENUM) ||
                    //     classFileType.equals(ClassFileType.ANNOTATION) ||
                    //     classFileType.equals(ClassFileType.CONSTANT) ||
                    //     classFileType.equals(ClassFileType.INTERFACE) ||
                    //     classFileType.equals(ClassFileType.EXCEPTION)) {
                    //     myFileWriter.writeDebloatReport(UsageStatusEnum.PRESERVED_CLASS.getName(),
                    //         currentClassName, classFileType);
                    //     myFileWriter.writePreservedClass(currentClassName, classFileType);
                    // } else {
                        // Remove the class
                        LOGGER.info("Removed class: " + currentClassName);
                        // Get the current directory
                        File parent = new File(classFile.getParent());
                        // Write report
                        myFileWriter.writeDebloatReport(UsageStatusEnum.BLOATED_CLASS.getName(),
                            currentClassName, classFileType);
                        Files.delete(classFile.toPath());
                        nbClassesRemoved++;
                        // Remove the parent folder if is empty
                        while (parent.isDirectory() && Objects.requireNonNull(parent.listFiles()).length == 0) {
                            deleteDirectory(parent);
                            parent = parent.getParentFile();
                        }
                    // }
                } else {
                    myFileWriter.writeDebloatReport(UsageStatusEnum.USED_CLASS.getName(), currentClassName, classFileType);
                }
            }
        }
    }


    private ClassFileType getClassFileType(URLClassLoader urlClassLoader, String currentClassName, String classFilePath)
        throws FileNotFoundException
    {
        CustomClassReader ccr = new CustomClassReader(new FileInputStream(classFilePath));
        ClassFileType classFileType = ClassFileType.CLASS;
        try {
            if (urlClassLoader != null) {
                Class<?> clazz = urlClassLoader.loadClass(currentClassName);
                if (clazz.isAnnotation()) {
                    classFileType = ClassFileType.ANNOTATION;
                } else if (clazz.isInterface() || ccr.isInterface()) {
                    classFileType = ClassFileType.INTERFACE;
                } else if (clazz.isEnum()) {
                    classFileType = ClassFileType.ENUM;
                } else if (Modifier.isFinal(clazz.getModifiers())) {
                    classFileType = ClassFileType.CONSTANT;
                } else if (ccr.isException()) {
                    classFileType = ClassFileType.EXCEPTION;
                } else if (Modifier.isAbstract(clazz.getModifiers())) {
                    classFileType = ClassFileType.CLASS_ABSTRACT;
                } else {
                    try {
                        boolean allPrivate = true;
                        for (int i = 0; i < clazz.getConstructors().length; ++i) {
                            allPrivate = allPrivate && Modifier.isPrivate(clazz.getConstructors()[i].getModifiers());
                        }
                        if (allPrivate) {
                            classFileType = ClassFileType.SINGLETON;
                        } else {
                            boolean allStatic = true;
                            for (Field field : clazz.getFields()) {
                                allStatic = allStatic && Modifier.isStatic(field.getModifiers());
                            }
                            for (Method method : clazz.getMethods()) {
                                allStatic = allStatic && Modifier.isStatic(method.getModifiers());
                            }
                            if (allStatic) {
                                classFileType = ClassFileType.CONSTANT;
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error preserving constant class: " + currentClassName);
                    }
                }
            }
        } catch (Throwable e) {
            // ignore
            classFileType = ClassFileType.UNKNOWN;
        }
        return classFileType;
    }

    private String getBinaryNameOfClassFiles(final String classFilePath, final String dirPath)
    {
        return classFilePath
            .replaceAll("/", ".")
            .substring(dirPath.length() + 1, classFilePath.length() - 6);
    }

    public int nbClassesRemoved()
    {
        return nbClassesRemoved;
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
