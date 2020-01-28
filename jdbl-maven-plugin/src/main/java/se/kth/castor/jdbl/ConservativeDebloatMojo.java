package se.kth.castor.jdbl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Maven mojo statically instruments the project and its dependencies in order to remove unused API members.
 */
@Mojo(name = "conservative-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class ConservativeDebloatMojo extends AbstractMojo {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    /**
     * The maven home file, assuming either an environment variable M2_HOME, or that mvn command exists in PATH.
     */
    private static final File mavenHome = new File(System.getenv().get("M2_HOME"));

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    @Override
    public void execute() {

        String outputDirectory = project.getBuild().getOutputDirectory();
        File baseDir = project.getBasedir();

        getLog().info("***** STARTING CONSERVATIVE DEBLOAT *****");

        se.kth.castor.jdbl.util.MavenUtils mavenUtils = new se.kth.castor.jdbl.util.MavenUtils(mavenHome, baseDir);

        // copy the dependencies
        mavenUtils.copyDependencies(outputDirectory);

        // copy the resources
        mavenUtils.copyResources(outputDirectory);

        // decompress the copied dependencies
        se.kth.castor.jdbl.util.JarUtils.decompressJars(outputDirectory);

        se.kth.castor.jdbl.callgraph.JCallGraphModified jCallGraphModified = new se.kth.castor.jdbl.callgraph.JCallGraphModified();

        // run de static usage analysis
        Map<String, Set<String>> usageAnalysis = jCallGraphModified.runUsageAnalysis(project.getBuild().getOutputDirectory());
        Set<String> classesUsed = usageAnalysis.keySet();

        getLog().info("#Total classes: " + usageAnalysis.entrySet().stream().count());
        getLog().info("#Unused classes: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count());
        getLog().info("#Unused methods: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() != null).map(Map.Entry::getValue).mapToInt(Set::size).sum());

        // delete unused classes
        se.kth.castor.jdbl.util.FileUtils fileUtils = new se.kth.castor.jdbl.util.FileUtils(outputDirectory, new HashSet<>(), classesUsed);
        try {
            fileUtils.deleteUnusedClasses(outputDirectory);
        } catch (IOException e) {
            getLog().error("Error deleting unused classes: " + e);
        }

        // delete unused methods
        se.kth.castor.jdbl.debloat.AbstractMethodDebloat conservativeMethodDebloat = new se.kth.castor.jdbl.debloat.ConservativeMethodDebloat(outputDirectory, usageAnalysis);
        try {
            conservativeMethodDebloat.removeUnusedMethods();
        } catch (IOException e) {
            getLog().error("Error: " + e);
        }

        getLog().info("***** CONSERVATIVE_DEBLOAT SUCCESS *****");
    }
}
