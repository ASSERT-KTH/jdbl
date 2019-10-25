package se.kth.jdbl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;
import se.kth.jdbl.debloat.AbstractMethodDebloat;
import se.kth.jdbl.debloat.EntryPointMethodDebloat;
import se.kth.jdbl.util.ClassesLoadedSingleton;
import se.kth.jdbl.util.FileUtils;
import se.kth.jdbl.util.JarUtils;
import se.kth.jdbl.util.MavenUtils;
import se.kth.jdbl.wrapper.DebloatTypeEnum;
import se.kth.jdbl.wrapper.JacocoWrapper;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Maven mojo instruments the project according to an entry point provided as parameters in Maven configuration.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered elements are removed from the final bundled jar file.
 */
@Mojo(name = "entry-point-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class EntryPointDebloatMojo extends AbstractMojo {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    /**
     * The maven home file, assuming either an environment variable M2_HOME, or that mvn command exists in PATH.
     */
    private static final File mavenHome = new File(System.getenv().get("M2_HOME"));

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "entry.class", name = "entryClass", required = true)
    private String entryClass = "";

    @Parameter(property = "entry.method", name = "entryMethod", required = true)
    private String entryMethod = "";

    @Parameter(property = "entry.parameters", name = "entryParameters", defaultValue = " ")
    private String entryParameters = null;

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    @Override
    public void execute() {

        String outputDirectory = project.getBuild().getOutputDirectory();
        File baseDir = project.getBasedir();


        getLog().info("***** STARTING DEBLOAT FROM ENTRY POINT *****");

        MavenUtils mavenUtils = new MavenUtils(mavenHome, baseDir);

        // copy the dependencies
        mavenUtils.copyDependencies(outputDirectory);

        // copy the resources
        mavenUtils.copyResources(outputDirectory);

        // decompress the copied dependencies
        JarUtils.decompressJars(outputDirectory);

        // getting the used methods
        JacocoWrapper jacocoWrapper = new JacocoWrapper(
                project,
                new File(project.getBasedir().getAbsolutePath() + "/target/report.xml"),
                DebloatTypeEnum.ENTRY_POINT_DEBLOAT,
                entryClass,
                entryMethod,
                entryParameters,
                mavenHome);

        Map<String, Set<String>> usageAnalysis = null;

        // run the usage analysis
        try {
            usageAnalysis = jacocoWrapper.analyzeUsages();
            // print some results
            getLog().info("#Unused classes: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count());
            getLog().info("#Unused methods: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() != null).map(Map.Entry::getValue).mapToInt(Set::size).sum());
        } catch (IOException | ParserConfigurationException | SAXException e) {
            getLog().error(e);
        }

        // delete unused classes
        FileUtils fileUtils = new FileUtils(outputDirectory, new HashSet<>(), ClassesLoadedSingleton.INSTANCE.getClassesLoaded());
        try {
            fileUtils.deleteUnusedClasses(outputDirectory);
        } catch (IOException e) {
            getLog().error("Error deleting unused classes: " + e);
        }

        // delete unused methods
        AbstractMethodDebloat entryPointMethodDebloat = new EntryPointMethodDebloat(outputDirectory, usageAnalysis);
        try {
            entryPointMethodDebloat.removeUnusedMethods();
        } catch (IOException e) {
            getLog().error("Error: " + e);
        }

        getLog().info("***** DEBLOAT FROM FROM ENTRY POINT SUCCESS *****");
    }
}
