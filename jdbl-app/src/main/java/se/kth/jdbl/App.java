package se.kth.jdbl;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.IParameterSplitter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

public class App {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    @Parameter(names = {"--input", "-i"}, converter = FileConverter.class, description = "File path to jar with dependencies", order = 1)
    private static File inputFilePath = null;

    @Parameter(names = {"--output", "-o"}, converter = FileConverter.class, description = "File path to the debloated jar with dependencies", order = 2)
    private static File outputFilePath = null;

    @Parameter(names = {"--class", "-c"}, description = "Fully qualified name of the entry point class", order = 3)
    private static String entryClass = null;

    @Parameter(names = {"--method", "-m"}, description = "Entry point method name", order = 4)
    private static String entryMethod = null;

    @Parameter(names = {"--params", "-p"}, splitter = SemiColonSplitter.class, description = "Parameters to pass to the entry point method", order = 5)
    private static List<String> entryParams = null;

    @Parameter(names = {"-exclude", "-e"}, splitter = SemiColonSplitter.class, description = "List of classes to exclude, i.e those classes will not be debloated", order = 5)
    private static List<String> exclusionList = null;

    @Parameter(names = {"--help", "-h"}, help = true, description = "Show the commands' help", order = 6)
    private static boolean help;

    private static DebloatBuilder debloatBuilder;

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(App.class.getName());

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    public static void main(String[] args) {
        App app = new App();

        JCommander.newBuilder()
                .addObject(app)
                .build()
                .parse(args);

        // construct the debloat object
        debloatBuilder = new DebloatBuilder();
        debloatBuilder.addInputFilePath(inputFilePath)
                .addOutputFilePath(outputFilePath)
                .addEntryClass(entryClass)
                .addEntryMethod(entryMethod)
                .addEntryParam(entryParams)
                .build();

        if (help) { // print the usage
            JCommander.newBuilder()
                    .programName("App")
                    .addObject(app)
                    .build()
                    .usage();
        } else if (inputsAreNotNull()) {
            app.runDebloat();
        }
    }

    //--------------------------------/
    //------ PRIVATE METHOD/S -------/
    //------------------------------/

    private static boolean inputsAreNotNull() {
        return inputFilePath != null && outputFilePath != null && entryClass != null && entryMethod != null && entryParams != null;
    }

    /**
     * Debloat a jar file received as parameters.
     *
     * @throws IOException
     */
    private void runDebloat() throws IOException {
        // start debloat from entry point
        EntryPointDebloat entryPointDebloat = new EntryPointDebloat(debloatBuilder);
        entryPointDebloat.debloat(new JarFile(debloatBuilder.getInputFilePath()));
    }

    private void printUsage(String[] args) {
        System.err.println("Incorrect agent arguments. Argument must belong to the following list (and be separated by space)");
        System.err.println("\t\t- includes=org.package(,org.package2)* Default: Empty");
        System.err.println("\t\t- excludes=org.package(,org.package2)* Default: fr.inria.yajta");
        System.err.println("\t\t- isotopes=org.package(,org.package2)* Default:Empty");
        System.err.println("\t\t- print=(list,tree,tie,values,branch,count) Default: tree");
        System.err.println("\t\t- strict-includes Default: false");
        System.err.println("\t\t- follow=File Default: null");
        System.err.println("\t\t- mfollow=File Default: null");
        System.err.println("\t\t- output=File Default: null");
        System.err.println("Found: \"" + Arrays.toString(args) + "\"");
    }

    /**
     * String parameter conversion to file type.
     */
    class FileConverter implements IStringConverter<File> {
        @Override
        public File convert(String value) {
            return new File(value);
        }
    }

    /**
     * Split list parameters.txt by semicolons.
     */
    class SemiColonSplitter implements IParameterSplitter {
        public List<String> split(String value) {
            return Arrays.asList(value.split(";"));
        }
    }
}


