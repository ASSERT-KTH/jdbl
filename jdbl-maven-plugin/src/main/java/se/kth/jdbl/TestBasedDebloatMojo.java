package se.kth.jdbl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;
import se.kth.jdbl.debloat.AbstractMethodDebloat;
import se.kth.jdbl.debloat.TestBasedMethodDebloat;
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
 * This Mojo instruments the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered elements are removed from the final jar file.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractMojo {

    /**
     * The maven home file, assuming either an environment variable M2_HOME, or that mvn command exists in PATH.
     */
    private static final File mavenHome = new File(System.getenv().get("M2_HOME"));

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() {

        String outputDirectory = project.getBuild().getOutputDirectory();
        File baseDir = project.getBasedir();

        getLog().info("***** STARTING TESTS BASED DEBLOAT *****");

        MavenUtils mavenUtils = new MavenUtils(mavenHome, baseDir);

        // copy the dependencies
        mavenUtils.copyDependencies(outputDirectory);

        // copy the resources
        mavenUtils.copyResources(outputDirectory);

        // decompress the copied dependencies
        JarUtils.decompressJars(outputDirectory);

        JacocoWrapper jacocoWrapper = new JacocoWrapper(project,
                new File(project.getBasedir().getAbsolutePath() + "/target/report.xml"),
                DebloatTypeEnum.TEST_DEBLOAT);

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
        AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(outputDirectory, usageAnalysis);
        try {
            testBasedMethodDebloat.removeUnusedMethods();
        } catch (IOException e) {
            getLog().error("Error: " + e);
        }

        // print information
        getLog().info("TESTS BASED DEBLOAT SUCCESS");
    }

}
