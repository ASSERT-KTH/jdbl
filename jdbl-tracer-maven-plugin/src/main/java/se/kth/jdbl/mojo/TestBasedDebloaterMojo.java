package se.kth.jdbl.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import se.kth.jdbl.debloater.DebloaterClassFileTransformer;
import se.kth.jdbl.instrumenter.MethodInstrumenterLogger;
import se.kth.jdbl.instrumenter.Instrumenter;
import se.kth.jdbl.instrumenter.InstrumenterClassFileTransformer;
import se.kth.jdbl.instrumenter.UsageRecorder;
import se.kth.jdbl.loader.TestBasedClassLoader;
import se.kth.jdbl.util.CustomLogger;
import se.kth.jdbl.util.FileUtils;
import se.kth.jdbl.util.JarUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * <p>
 * This Mojo instruments the project according to the test suite the before the Maven packaging phase.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered elements are removed from the final jar file.
 * </p>
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TestBasedDebloaterMojo extends AbstractMojo {

    private ArrayList<String> tests = new ArrayList<>();
    private static final String INSTRUMENTED_SUFFIX = "-instrumented";
    private static final String DEBLOATED_SUFFIX = "-debloated";


    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String inputDirectory = project.getBuild().getOutputDirectory();
        String testOutputDirectory = project.getBuild().getTestOutputDirectory();
        String outputDirectory = project.getBuild().getOutputDirectory();

        getLog().info("DEBLOAT FROM TESTS STARTED");

        // Decompress the jar files in the output directory
        JarUtils.decompressJars(outputDirectory);

        // Instrument the compiled classes
        Instrumenter instrumenter = new Instrumenter();
        instrumenter.setTransformer(new InstrumenterClassFileTransformer());
        instrumenter.setClassLoader(new InstrumenterClassFileTransformer(), testOutputDirectory);
        instrumenter.main(outputDirectory, outputDirectory + INSTRUMENTED_SUFFIX);

        // Reset the state of the logger
        MethodInstrumenterLogger.dump();

        //URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        ClassLoader cl;
        Class junitCore;
        File file = new File(testOutputDirectory + "/");
        File instrumentedClasses = new File(outputDirectory + INSTRUMENTED_SUFFIX);
        Class clazz = null;

        try {
            Class arrayClass = Class.forName("[Ljava.lang.Class;");

            // Create a new class loader with the directory
            //cl = new URLClassLoader(new URL[]{urlTests, urlInstrumented}, TestBasedDebloaterMojo.class.getClassLoader());
            cl = new TestBasedClassLoader(testOutputDirectory, outputDirectory + INSTRUMENTED_SUFFIX, TestBasedDebloaterMojo.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(cl);

            ArrayList<String> listTest = findTests(testOutputDirectory);

            getLog().info("Number of tests: " + listTest.size());

            // Execute all the test files
            for (String test : listTest) {
                // Load the test ClassLoader
                clazz = cl.loadClass(test);
                // Invoke the test cases
                junitCore = cl.loadClass("org.junit.runner.JUnitCore");
                Method methodRunClasses = junitCore.getMethod("runClasses", arrayClass);
                methodRunClasses.invoke(null, new Object[]{new Class[]{clazz}});
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            getLog().error("Error: " + e);
        }

        MethodInstrumenterLogger.dump();


        // Logs to files
        CustomLogger customLogger = new CustomLogger();
        try {
            customLogger.logElementRemoved(project.getBuild().getDirectory() + "/" + "used_classes.log", UsageRecorder.getUsedClasses());
            customLogger.logElementRemoved(project.getBuild().getDirectory() + "/" + "used_methods.log", UsageRecorder.getUsedMethods());
        } catch (IOException e) {
            System.err.println(e);
        }

        // Remove the untested classesUsed before removing the methods
        FileUtils.classesUsed = UsageRecorder.getUsedClasses();
        FileUtils.outputDirectory = inputDirectory;
        try {
            FileUtils.deleteUnusedClasses(inputDirectory);
        } catch (IOException e) {
            getLog().error("Error: " + e);
        }

        // Instrument the classesUsed
        instrumenter = new Instrumenter();
        DebloaterClassFileTransformer debloaterClassFileTransformer = new DebloaterClassFileTransformer();
        instrumenter.setTransformer(debloaterClassFileTransformer);
        instrumenter.setClassLoader(debloaterClassFileTransformer, inputDirectory);
        instrumenter.main(inputDirectory, inputDirectory + DEBLOATED_SUFFIX);

        // Logs to standard output
        System.out.println("Number of classes instrumented: " + instrumenter.getNbClassesInstrumented());
        System.out.println("Number of classes removed: " + FileUtils.classesRemoved);
        System.out.println("Number of methods removed: " + debloaterClassFileTransformer.getNbMethodsRemoved());

        // Delete the classesUsed directory
        FileUtils.renameFolder(inputDirectory, inputDirectory + "-original");

        // Rename the classesUsed-debloated directory to classesUsed
        FileUtils.renameFolder(inputDirectory + DEBLOATED_SUFFIX, inputDirectory);

        getLog().info("DEBLOAT FROM TESTS SUCCESS");
    }

    /**
     * Recursively search class files in a directory.
     *
     * @param testOutputDirectory
     * @return the name of tests files present in a given directory.
     */
    private ArrayList<String> findTests(String testOutputDirectory) {
        File f = new File(testOutputDirectory);
        File[] list = f.listFiles();
        for (File testFile : list) {
            if (testFile.isDirectory()) {
                findTests(testFile.getAbsolutePath());
            } else if (testFile.getName().endsWith(".class")) {
                String testName = testFile.getAbsolutePath();
                // Get the binary name of the test file
                tests.add(testName.replaceAll("/", ".")
                        .substring(project.getBuild().getTestOutputDirectory().length() + 1, testName.length() - 6));
            }
        }
        return tests;
    }
}
