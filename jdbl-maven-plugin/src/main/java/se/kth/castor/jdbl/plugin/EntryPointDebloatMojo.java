package se.kth.castor.jdbl.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import se.kth.castor.jdbl.coverage.AbstractCoverage;
import se.kth.castor.jdbl.coverage.JVMClassesCoveredSingleton;
import se.kth.castor.jdbl.coverage.JacocoCoverage;
import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.debloat.EntryPointMethodDebloat;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;
import se.kth.castor.jdbl.util.MyFileUtils;

/**
 * This Maven mojo instruments the project according to an entry point provided as parameters in Maven configuration.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered elements are removed from the final bundled jar file.
 */
@Mojo(name = "entry-point-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class EntryPointDebloatMojo extends AbstractDebloatMojo
{
    @Parameter(property = "entry.class", name = "entryClass", required = true)
    private String entryClass = "";

    @Parameter(property = "entry.method", name = "entryMethod", required = true)
    private String entryMethod = "";

    @Parameter(property = "entry.parameters", name = "entryParameters", defaultValue = " ")
    private String entryParameters = null;

    @Override
    public void doExecute()
    {
        printCustomStringToConsole("S T A R T I N G    E N T R Y    P O I N T    D E B L O A T");

        String outputDirectory = getProject().getBuild().getOutputDirectory();
        File baseDir = getProject().getBasedir();

        MavenUtils mavenUtils = new MavenUtils(getMavenHome(), baseDir);

        // copy the dependencies
        mavenUtils.copyDependencies(outputDirectory);

        // copy the resources
        mavenUtils.copyResources(outputDirectory);

        // decompress the copied dependencies
        JarUtils.decompressJars(outputDirectory);

        // getting the used methods
        AbstractCoverage jacocoCoverage = new JacocoCoverage(
            getProject(),
            mavenHome,
            DebloatTypeEnum.ENTRY_POINT_DEBLOAT,
            this.entryClass,
            this.entryMethod,
            this.entryParameters);

        UsageAnalysis usageAnalysis = null;

        // run the usage analysis
        usageAnalysis = jacocoCoverage.analyzeUsages();
        // print some results
        this.getLog().info(String.format("#Unused classes: %d",
            usageAnalysis.getAnalysis().entrySet().stream().filter(e -> e.getValue() == null).count()));
        this.getLog().info(String.format("#Unused methods: %d",
            usageAnalysis.getAnalysis().values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));

        // remove unused classes
        MyFileUtils myFileUtils = new MyFileUtils(outputDirectory, new HashSet<>(),
            JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded(),
            getProject().getBasedir().getAbsolutePath(), null);
        try {
            myFileUtils.deleteUnusedClasses(outputDirectory);
        } catch (IOException e) {
            this.getLog().error(String.format("Error deleting unused classes: %s", e));
        }

        // remove unused methods
        AbstractMethodDebloat entryPointMethodDebloat = new EntryPointMethodDebloat(outputDirectory,
            usageAnalysis,
            getProject().getBasedir().getAbsolutePath() + "/" + getReportFileName());
        try {
            entryPointMethodDebloat.removeUnusedMethods();
        } catch (IOException e) {
            this.getLog().error(String.format("Error: %s", e));
        }
        printCustomStringToConsole("E N T R Y    P O I N T    D E B L O A T    F I N I S H E D");
    }
}


