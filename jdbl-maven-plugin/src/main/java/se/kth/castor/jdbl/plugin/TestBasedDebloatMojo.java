package se.kth.castor.jdbl.plugin;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import se.kth.castor.jdbl.coverage.CoverageToolEnum;
import se.kth.castor.jdbl.coverage.JCovCoverage;
import se.kth.castor.jdbl.coverage.JVMClassCoverage;
import se.kth.castor.jdbl.coverage.JVMClassesCoveredSingleton;
import se.kth.castor.jdbl.coverage.JacocoCoverage;
import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.coverage.YajtaCoverage;
import se.kth.castor.jdbl.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.test.StackLine;
import se.kth.castor.jdbl.test.TestResultReader;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.MyFileUtils;
import se.kth.castor.jdbl.util.MyFileWriter;

/**
 * This Mojo debloats the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered classes are removed from the final jar file, the non covered
 * methods are replaced by an <code>UnsupportedOperationException</code>.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractDebloatMojo
{
    @Override
    public void doExecute()
    {
        printCustomStringToConsole("JDBL: STARTING TEST BASED DEBLOAT");
        Instant start = Instant.now();
        String outputDirectory = getProject().getBuild().getOutputDirectory();
        String projectBaseDir = getProject().getBasedir().getAbsolutePath();
        MyFileWriter myFileWriter = new MyFileWriter(projectBaseDir);
        myFileWriter.resetJDBLReportsDirectory();

        // Run JCov usage analysis
        JCovCoverage jcovCoverage = new JCovCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jcovUsageAnalysis = jcovCoverage.analyzeUsages();

        // Run yajta usage analysis
        YajtaCoverage yajtaCoverage = new YajtaCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis yajtaUsageAnalysis = yajtaCoverage.analyzeUsages();

        // Run JaCoCo usage analysis
        JacocoCoverage jacocoCoverage = new JacocoCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jacocoUsageAnalysis = jacocoCoverage.analyzeUsages();

        // Run JVM class usage analysis
        JVMClassCoverage jvmClassCoverage = new JVMClassCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jvmUsageAnalysis = jvmClassCoverage.analyzeUsages();


        // Print out JCov coverage output
        System.out.println("JCov:");
        if (!jcovUsageAnalysis.classes().isEmpty() && yajtaUsageAnalysis != null) {
            System.out.print(jcovUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JCOV, jcovUsageAnalysis);
        printCoverageAnalysisResults(jcovUsageAnalysis);

        // Print out Yajta coverage output
        System.out.println("Yajta:");
        if (!yajtaUsageAnalysis.classes().isEmpty() && yajtaUsageAnalysis != null) {
            System.out.print(yajtaUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.YAJTA, yajtaUsageAnalysis);
        printCoverageAnalysisResults(yajtaUsageAnalysis);

        // Print out JaCoCo coverage output
        System.out.println("JaCoCo:");
        if (!jacocoUsageAnalysis.classes().isEmpty() && jacocoUsageAnalysis != null) {
            System.out.print(jacocoUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JACOCO, jacocoUsageAnalysis);
        printCoverageAnalysisResults(jacocoUsageAnalysis);

        // Print out JVM coverage output
        System.out.println("JVM:");
        if (!jvmUsageAnalysis.classes().isEmpty() && jvmUsageAnalysis != null) {
            System.out.print(jvmUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JVM_CLASS_LOADER, jvmUsageAnalysis);
        printCoverageAnalysisResults(jvmUsageAnalysis);

        // Merge the coverage analysis
        UsageAnalysis mergedUsageAnalysis = yajtaUsageAnalysis.mergeWith(jacocoUsageAnalysis).mergeWith(jcovUsageAnalysis);

        // Get the classes loaded by the JVM
        Set<String> usedClasses = null;
        try {
            this.printClassesLoaded();
            usedClasses = TestBasedDebloatMojo.getUsedClasses(mergedUsageAnalysis);
        } catch (RuntimeException e) {
            this.getLog().error("Error computing JaCoCo usage analysis.");
        }

        // Read the failing methods from the stacktrace
        TestResultReader testResultReader = new TestResultReader(".");
        Set<StackLine> failingMethods = testResultReader.getMethodFromStackTrace();
        for (StackLine failingMethod : failingMethods) {
            if (usedClasses != null) {
                usedClasses.add(failingMethod.getClassName());
            }
        }

        // Write to a file with the status of classes (Used or Bloated) in each dependency
        try {
            myFileWriter.writeClassStatusPerDependency(usedClasses);
        } catch (RuntimeException e) {
            this.getLog().error("Error writing the status of classes per dependency.");
        }

        // ----------------------------------------------------
        this.getLog().info("Starting removing unused classes...");
        this.removeUnusedClasses(outputDirectory, usedClasses);

        // ----------------------------------------------------
        this.getLog().info("Starting removing unused methods...");
        this.removeUnusedMethods(outputDirectory, mergedUsageAnalysis, failingMethods);

        // ----------------------------------------------------
        try {
            this.getLog().info("Starting running the test suite on the debloated version...");
            TestRunner.runTests(getProject(), false);
        } catch (IOException e) {
            this.getLog().error("IOException when rerunning the tests");
        }

        // ----------------------------------------------------
        myFileWriter.writeTimeElapsedReportFile(start);
        printCustomStringToConsole("JDBL: TEST BASED DEBLOAT FINISHED");
    }

    private void printClassesLoaded()
    {
        final int nbOfClassesLoaded = JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded().size();
        this.getLog().info("Loaded classes (" + nbOfClassesLoaded + ')');
        JVMClassesCoveredSingleton.INSTANCE.printClassesLoaded();
        this.getLog().info(getLineSeparator());
    }

    private void removeUnusedMethods(final String outputDirectory, final UsageAnalysis usageAnalysis,
        Set<StackLine> failingMethods)
    {
        AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(outputDirectory,
            usageAnalysis,
            getProject().getBasedir().getAbsolutePath(), failingMethods);
        try {
            testBasedMethodDebloat.removeUnusedMethods();
            this.getLog().info("Total methods in used classes removed: " + testBasedMethodDebloat.nbMethodsRemoved());
        } catch (IOException e) {
            this.getLog().error(String.format("Error: %s", e));
        }
    }

    private void removeUnusedClasses(final String outputDirectory, final Set<String> usedClasses)
    {
        try {
            MyFileUtils myFileUtils = new MyFileUtils(outputDirectory,
                new HashSet<>(),
                usedClasses,
                getProject().getBasedir().getAbsolutePath(),
                getProject().getTestClasspathElements());

            myFileUtils.deleteUnusedClasses(outputDirectory);

            // Delete bloated classes in the JAR with dependencies
            myFileUtils.deleteUnusedClassesInJarWithDependencies(getProject().getBasedir().getAbsolutePath());

            this.getLog().info("Total classes removed: " + myFileUtils.nbClassesRemoved());
        } catch (Exception e) {
            this.getLog().error(String.format("Error deleting unused classes: %s", e));
        }
    }

    private static Set<String> getUsedClasses(final UsageAnalysis usageAnalysis)
    {
        // get the union of the JaCoCo output and the JVM class loader results
        Set<String> usedClasses = new HashSet<>(JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded());
        usageAnalysis
            .getAnalysis()
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .forEach(className -> usedClasses.add(className.getKey().replace('/', '.')));
        return usedClasses;
    }

    private void printCoverageAnalysisResults(final UsageAnalysis usageAnalysis)
    {
        this.getLog().info("ANALYSIS RESULTS:");
        this.getLog().info(String.format("Total used classes: %d",
            usageAnalysis.getAnalysis().entrySet().stream().filter(e -> e.getValue() != null).count()));
        this.getLog().info(String.format("Total used methods: %d",
            usageAnalysis.getAnalysis().values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));
        this.getLog().info(getLineSeparator());
    }
}
