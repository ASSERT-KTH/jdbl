package se.kth.jdbl.wrapper;

import se.kth.jdbl.loader.TestBasedClassLoader;
import se.kth.jdbl.TestBasedDebloatMojo;
import se.kth.jdbl.util.JarUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class ClassLoaderTest {

    private static ArrayList<String> tests = new ArrayList<>();
    private static String testOutputDirectory = "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/clitools/target/test-classes";
    private static String outputDirectory = "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/clitools/target/classes";

    public static void main(String[] args) {

        JarUtils.decompressJars("/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/clitools/target/classes");

        ClassLoader cl = null;
        Class clazz;
        Class junitCore;

        try {
            Class arrayClass = Class.forName("[Ljava.lang.Class;");

            // Create a new class loader with the directory
            cl = new TestBasedClassLoader(testOutputDirectory, outputDirectory, TestBasedDebloatMojo.class.getClassLoader());

            Thread.currentThread().setContextClassLoader(cl);

//            ArrayList<String> testsFiles = findTestFiles(testOutputDirectory);

            ArrayList<String> testsFiles = new ArrayList<>();
            testsFiles.add("com.sangupta.clitools.CliMainTest");

            System.out.println("Number of test classes: " + testsFiles.size());

            // Execute all the test files
            for (String test : testsFiles) {
                // Load the test ClassLoader
                clazz = cl.loadClass(test);
                // Invoke the test cases
                junitCore = cl.loadClass("org.junit.runner.JUnitCore");
                Method methodRunClasses = junitCore.getMethod("runClasses", arrayClass);
                methodRunClasses.invoke(null, new Object[]{new Class[]{clazz}});
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            System.out.println("Error: " + e);
        }

        cl = Thread.currentThread().getContextClassLoader();

        while (cl != null) {
            System.out.println("ClassLoader: " + cl);
            try {
                for (Iterator iter = list(cl); iter.hasNext(); ) {
                    System.out.println("\t" + iter.next());
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            cl = cl.getParent();
        }
    }

    private static Iterator list(ClassLoader CL)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Class CL_class = CL.getClass();
        while (CL_class != java.lang.ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        java.lang.reflect.Field ClassLoader_classes_field = CL_class
                .getDeclaredField("classes");
        ClassLoader_classes_field.setAccessible(true);
        Vector classes = (Vector) ClassLoader_classes_field.get(CL);
        return classes.iterator();
    }

    /**
     * Recursively search class files in a directory.
     *
     * @param testOutputDirectory
     * @return the name of tests files present in a given directory.
     */
    private static ArrayList<String> findTestFiles(String testOutputDirectory) {
        File f = new File(testOutputDirectory);
        File[] list = f.listFiles();
        for (File testFile : list) {
            if (testFile.isDirectory()) {
                findTestFiles(testFile.getAbsolutePath());
            } else if (testFile.getName().endsWith(".class")) {
                String testName = testFile.getAbsolutePath();
                // Get the binary name of the test file
                tests.add(testName.replaceAll("/", ".")
                        .substring(testOutputDirectory.length() + 1, testName.length() - 6));
            }
        }
        return tests;
    }

}
