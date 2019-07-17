package se.kth.jdbl.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class FileUtils {

    private FileUtils() {
    }

    /**
     * Counts the number of classes removed.
     */
    public static int classesRemoved = 0;

    /**
     * The build outputDirectory.
     */
    public static String outputDirectory;

    /**
     * Exclusion list of classes and package names that should not be removed.
     */
    public static Set<String> exclusionSet = new HashSet<>();

    /**
     * Set of the binary names of classesUsed traced.
     */
    public static HashSet<String> classesUsed;


    /**
     * Feed the list of non-removable classes.
     *
     * @param pathToFile
     */
    public static void setExclusionList(String pathToFile) {
        Path path = Paths.get(pathToFile);
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(s -> exclusionSet.add(s.replaceAll("/", ".")));
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Recursively remove classes that were not traced.
     *
     * @param currentPath the start file path to delete.
     */
    public static void deleteUnusedClasses(String currentPath) throws IOException {
        File f = new File(currentPath);
        File[] list = f.listFiles();
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
                // check if we can remove the class safely
                if (!classesUsed.contains(currentClassName) && !exclusionSet.contains(currentClassName)) {
                    // get the current directory
                    File parent = new File(classFile.getParent());
                    // remove the file
                    classFile.delete();
                    classesRemoved++;
                    // remove the parent folder if is empty
                    while (parent.isDirectory() && parent.listFiles().length == 0) {
                        deleteDirectory(parent);
                        parent = parent.getParentFile();
                    }
                }
            }
        }
    }

    /**
     * Rename a folder in a given path.
     *
     * @param currentFolderPath
     * @param newNameFolderPath
     */
    public static boolean renameFolder(String currentFolderPath, String newNameFolderPath) {
        File file = new File(currentFolderPath);
        return file.renameTo(new File(newNameFolderPath));
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void deleteDirectory(final File directory) throws IOException {
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


    private static boolean isSymlink(final File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        return Files.isSymbolicLink(file.toPath());
    }

    private static void cleanDirectory(final File directory) throws IOException {
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

    private static File[] verifiedListFiles(final File directory) throws IOException {
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

    private static void forceDelete(final File file) throws IOException {
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
