package se.kth.jdbl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import se.kth.jdbl.util.ClassesLoadedSingleton;
import se.kth.jdbl.util.CmdExec;
import se.kth.jdbl.util.JarUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class EntryPointDebloat implements Debloat {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private DebloatBuilder debloatBuilder;

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(EntryPointDebloat.class.getName());

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public EntryPointDebloat(DebloatBuilder debloatBuilder) {
        this.debloatBuilder = new DebloatBuilder();
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    @Override
    public File debloat(File input) throws IOException {
        String inputPath = input.getAbsolutePath();
        String outputPath = input.getAbsolutePath() + File.separator + "original-classes";
        JarUtils.decompressJarFile(inputPath, outputPath);

        // instrument the classes
        instrumentClasses(new File(outputPath));
        // execute the application from the entry point
        executeEntryPoint(new File(outputPath));

        return null;
    }

    /**
     * Call jacoco to instrument the classes. Probes are inserted in order to get coverage.
     *
     * @param classesDir The directory of classes to be instrumented.
     */
    private void instrumentClasses(File classesDir) {
        if (!classesDir.exists()) {
            LOGGER.error("Skipping JaCoCo execution due to missing classes directory:" + classesDir);
            return;
        }

        final File originalClassesDir = new File(classesDir.getAbsolutePath(), "generated-classes/jacoco");

        List<String> fileNames = Arrays.asList(classesDir.list());

        final Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        for (final String fileName : fileNames) {
            if (fileName.endsWith(".class")) {
                final File source = new File(classesDir, fileName);
                final File backup = new File(originalClassesDir, fileName);
                InputStream input = null;
                OutputStream output = null;
                try {
                    FileUtils.copyFile(source, backup);
                    input = new FileInputStream(backup);
                    output = new FileOutputStream(source);
                    instrumenter.instrument(input, output, source.getPath());
                } catch (final IOException e) {
                    LOGGER.error("Unable to instrument file.", e);
                } finally {
                    IOUtil.close(input);
                    IOUtil.close(output);
                }
            }
        }
    }

    private void executeEntryPoint(File classesDir) {
        CmdExec cmdExec = new CmdExec();
        LOGGER.info("entryClass: ", debloatBuilder.getEntryClass());
        LOGGER.info("entryMethod: ", debloatBuilder.getEntryMethod());
        LOGGER.info("entryParameters: ", debloatBuilder.getEntryParams());
        LOGGER.info("Starting debloat from entry point");

        // add jacoco to the classpath
        String jacocoClasspath = addJacocoToClassPath(classesDir);

        // execute the application from the given entry point, we need to add jacoco to the classpath in order to
        // retrieve the information from the previously inserted probes
        Set<String> classesLoaded = cmdExec.execProcess(jacocoClasspath, debloatBuilder.getEntryClass(), debloatBuilder.getEntryParams().toArray(new String[0]));

        LOGGER.info("Number of classes loaded: " + classesLoaded.size());

        ClassesLoadedSingleton.INSTANCE.setClassesLoaded(classesLoaded);

        // list the classes loaded
        ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
    }

    private String addJacocoToClassPath(File classesDir){
        String jacocoClassPath = classesDir.getAbsolutePath() + File.separator + "jacoco";


        return jacocoClassPath

    }

}
