package se.kth.jdbl.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class FileUtils {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

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
     * Class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(FileUtils.class.getName());

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public FileUtils(String outputDirectory, Set<String> exclusionSet, Set<String> classesUsed) {
        this.nbClassesRemoved = 0;
        this.outputDirectory = outputDirectory;
        this.exclusionSet = exclusionSet;
        this.classesUsed = classesUsed;
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    /**
     * Feed the list of non-removable classes.
     *
     * @param pathToFile The exclusion list file which contains the list of classes that will not be deleted.
     */
    public void setExclusionList(String pathToFile) {
        Path path = Paths.get(pathToFile);
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(s -> exclusionSet.add(s.replaceAll("/", ".")));
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Recursively remove unused classes in a directory.
     *
     * @param currentPath the start file path to delete.
     */
    public void deleteUnusedClasses(String currentPath) throws IOException {
        File f = new File(currentPath);
        File[] list = f.listFiles();
        assert list != null;
        for (File classFile : list) {
            if (classFile.isDirectory()) {
                // recursive call for directories
                deleteUnusedClasses(classFile.getAbsolutePath());
            } else if (classFile.getName().endsWith(".class")) {
                String classFilePath = classFile.getAbsolutePath();

                // get the binary name of the test file
                String currentClassName = classFilePath
                        .replaceAll("/", ".")
                        .substring(outputDirectory.length() + 1, classFilePath.length() - 6);

                if (!classesUsed.contains(currentClassName) &&
                        isRemovable(currentClassName.replace("/", ".")) &&
                        !exclusionSet.contains(currentClassName)) {
                    // get the current directory
                    File parent = new File(classFile.getParent());
                    // remove the file
//                    LOGGER.info("Removing unused class: " + currentClassName);


                    Files.delete(classFile.toPath());
                    nbClassesRemoved++;
                    // remove the parent folder if is empty
                    while (parent.isDirectory() && Objects.requireNonNull(parent.listFiles()).length == 0) {
                        deleteDirectory(parent);
                        parent = parent.getParentFile();
                    }
                }
            }
        }
    }

    //--------------------------------/
    //------- GETTER METHOD/S -------/
    //------------------------------/

    public int getNbClassesRemoved() {
        return nbClassesRemoved;
    }

    //--------------------------------/
    //------ PRIVATE METHOD/S -------/
    //------------------------------/

    private boolean isRemovable(String className) throws IOException {
//
//        System.out.println("The classname: " + className);
//
//        boolean result;
//
//        BufferedReader reader;
//        try {
//            reader = new BufferedReader(new FileReader(
//                    "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/clitools/loaded-classes"));
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


    private void deleteDirectory(final File directory) throws IOException {
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

    private boolean isSymlink(final File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        return Files.isSymbolicLink(file.toPath());
    }

    private void cleanDirectory(final File directory) throws IOException {
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

    private File[] verifiedListFiles(final File directory) throws IOException {
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

    private void forceDelete(final File file) throws IOException {
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
