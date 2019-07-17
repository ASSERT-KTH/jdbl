package se.kth.jdbl.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import se.kth.jdbl.debloater.DebloaterClassFileTransformer;
import se.kth.jdbl.instrumenter.Instrumenter;
import se.kth.jdbl.instrumenter.InstrumenterClassFileTransformer;
import se.kth.jdbl.instrumenter.MethodInstrumenterLogger;
import se.kth.jdbl.instrumenter.UsageRecorder;
import se.kth.jdbl.loader.EntryPointClassLoader;
import se.kth.jdbl.util.CustomLogger;
import se.kth.jdbl.util.FileUtils;
import se.kth.jdbl.util.JarUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * This Mojo instruments the project from an entry point before the Maven packaging phase.
 * Probes are inserted in order to keep track of the classes and methods used.
 * </p>
 */
@Mojo(name = "entry-point-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class EntryPointDebloaterMojo extends AbstractMojo {

    private static final String DEBLOATED_SUFFIX = "-debloated";
    private static final String INSTRUMENTED_SUFFIX = "-instrumented";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "entry.class", name = "entryClass", required = true)
    private String entryClass = "";

    @Parameter(property = "entry.method", name = "entryMethod", required = true)
    private String entryMethod = "";

    @Parameter(property = "entry.parameters", name = "entryParameters", defaultValue = " ")
    private String entryParameters = null;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String inputDirectory = project.getBuild().getOutputDirectory();

        if (inputDirectory.endsWith("/")) {
            inputDirectory = inputDirectory.substring(0, inputDirectory.length() - 1);
        }

        // Start instrumenting the classesUsed
        getLog().info("DEBLOAT FROM ENTRY POINT STARTED");

        // Decompress the jar files in classesUsed
        JarUtils.decompressJars(project.getBuild().getOutputDirectory());

        // Instrument the compiled classes
        Instrumenter instrumenter = new Instrumenter();
        instrumenter.setTransformer(new InstrumenterClassFileTransformer());
        instrumenter.setClassLoader(new InstrumenterClassFileTransformer(), inputDirectory);
        instrumenter.main(inputDirectory, inputDirectory + INSTRUMENTED_SUFFIX);

        System.out.println("Number of classes instrumented: " + instrumenter.getNbClassesInstrumented());

        // Reset the state of the logger
        MethodInstrumenterLogger.dump();

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            EntryPointClassLoader entryPointClassLoader = new EntryPointClassLoader(new InstrumenterClassFileTransformer(), inputDirectory);
            Thread.currentThread().setContextClassLoader(entryPointClassLoader);

            String className = this.entryClass;
            Class entryClassLoaded = entryPointClassLoader.loadClass(className);

            Method entryMethodLoaded = entryClassLoaded.getDeclaredMethod(this.entryMethod, String[].class);

            if (this.entryParameters != null) {
                String[] parameters = this.entryParameters.split(" ");

                // start of logging block
                System.out.print("Invoking method {" + entryMethodLoaded.getName() + "} in class {" + entryClassLoaded.getName() + "} with parameters {");
                for (int i = 0; i < parameters.length - 1; i++) {
                    System.out.print(parameters[i] + ", ");
                }
                System.out.print(parameters[parameters.length - 1] + "}\n");
                // end of logging block

                entryMethodLoaded.invoke(null, (Object) parameters);
            } else {
                entryMethodLoaded.invoke(null, new Object[]{});
            }
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            getLog().error(e);
        }

        MethodInstrumenterLogger.dump();

        Thread.currentThread().setContextClassLoader(oldClassLoader);

        // Logs to files
        CustomLogger customLogger = new CustomLogger();
        try {
            customLogger.logElementRemoved(project.getBuild().getDirectory() + "/" + "used_classes.log", UsageRecorder.getUsedClasses());
            customLogger.logElementRemoved(project.getBuild().getDirectory() + "/" + "used_methods.log",  UsageRecorder.getUsedMethods());
        } catch (IOException e) {
            System.err.println(e);
        }

//        // Delete unused classes
//        FileUtils.classesUsed = UsageRecorder.getUsedClasses();
//        FileUtils.outputDirectory = inputDirectory;
//        FileUtils.setExclusionList("/home/cesarsv/Documents/papers/2019_papers/paper1/jdbl/jdbl-maven-plugin/src/main/resources/exclusion_list.txt");
//        System.out.println("Size of exclusion set: " + FileUtils.exclusionSet.size());
//        try {
//            FileUtils.deleteUnusedClasses(inputDirectory);
//        } catch (IOException e) {
//            getLog().error(e);
//        }
//
//        // Instrument the classes to remove unused methods
//        instrumenter = new Instrumenter();
//        DebloaterClassFileTransformer debloaterClassFileTransformer = new DebloaterClassFileTransformer();
//        instrumenter.setTransformer(debloaterClassFileTransformer);
//        instrumenter.setClassLoader(debloaterClassFileTransformer, inputDirectory);
//        instrumenter.main(inputDirectory, inputDirectory + DEBLOATED_SUFFIX);
//
//        // Logs to standard output
//        System.out.println("Number of classes removed: " + FileUtils.classesRemoved);
//        System.out.println("Number of methods removed: " + debloaterClassFileTransformer.getNbMethodsRemoved());
//
//        // Delete the classesUsed directory
//        FileUtils.renameFolder(inputDirectory, inputDirectory + "-original");
//
//        // Rename the classesUsed-debloated directory to classesUsed
//        FileUtils.renameFolder(inputDirectory + DEBLOATED_SUFFIX, inputDirectory);
//
//        // Instrumentation finished successfully
//        getLog().info("DEBLOAT FROM ENTRY POINT SUCCESS");
    }
}
