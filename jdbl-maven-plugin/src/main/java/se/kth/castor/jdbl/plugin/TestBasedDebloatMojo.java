package se.kth.castor.jdbl.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import se.kth.castor.jdbl.app.coverage.JacocoCoverage;
import se.kth.castor.jdbl.app.coverage.UsageAnalysis;
import se.kth.castor.jdbl.app.coverage.YajtaCoverage;
import se.kth.castor.jdbl.app.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.app.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.app.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.app.test.StackLine;
import se.kth.castor.jdbl.app.test.TestResultReader;
import se.kth.castor.jdbl.app.test.TestRunner;
import se.kth.castor.jdbl.app.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.app.util.JDblFileUtils;
import se.kth.castor.jdbl.app.util.JarUtils;

/**
 * This Mojo debloats the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered classes are removed from the final jar file, the non covered
 * methods are replaced by an <code>UnsupportedOperationException</code>.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractDebloatMojo
{
    @Override
    public void doExecute()
    {
        printCustomStringToConsole("JDBL: STARTING TEST BASED DEBLOAT");
        Instant start = Instant.now();
        cleanReportFile();
        String outputDirectory = getProject().getBuild().getOutputDirectory();

        // Run yajta analysis
        YajtaCoverage yajtaCoverage = new YajtaCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis yajtaUsageAnalysis = yajtaCoverage.analyzeUsages();

        // Run JaCoCo usage analysis
        JacocoCoverage jacocoCoverage = new JacocoCoverage(getProject(), mavenHome,
            new File(getProject().getBasedir().getAbsolutePath() + "/target/report.xml"), DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jacocoUsageAnalysis = jacocoCoverage.analyzeUsages();

        // Print Yajta and JaCoCo coverage outputs
        System.out.println("Yajta:");
        System.out.print(yajtaUsageAnalysis.toString());
        printCoverageAnalysisResults(yajtaUsageAnalysis);
        System.out.println("JaCoCo");
        System.out.print(jacocoUsageAnalysis.toString());
        printCoverageAnalysisResults(jacocoUsageAnalysis);

        // Merge coverage analysis
        UsageAnalysis mergedUsageAnalysis = mergeTwoUsageAnalysis(yajtaUsageAnalysis, jacocoUsageAnalysis);

        // Classes loaded
        Set<String> usedClasses = null;
        try {
            this.printClassesLoaded();
            usedClasses = TestBasedDebloatMojo.getUsedClasses(mergedUsageAnalysis);
        } catch (RuntimeException e) {
            this.getLog().error("Error computing JaCoCo usage analysis.");
        }

        TestResultReader testResultReader = new TestResultReader(".");
        Set<StackLine> failingMethods = testResultReader.getMethodFromStackTrace();
        for (StackLine failingMethod : failingMethods) {
            if (usedClasses != null) {
                usedClasses.add(failingMethod.getClassName());
            }
        }

        // Write to a file with the status of classes (Used or Bloated) in each dependency
        try {
            writeClassStatusPerDependency(usedClasses);
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
            TestRunner.runTests(getProject());
        } catch (IOException e) {
            this.getLog().error("IOException when rerunning the tests");
        }

        // ----------------------------------------------------
        writeTimeElapsedReportFile(start);
        printCustomStringToConsole("JDBL: TEST BASED DEBLOAT FINISHED");
    }

    private UsageAnalysis mergeTwoUsageAnalysis(UsageAnalysis aUsageAnalysis, UsageAnalysis anotherUsageAnalysis)
    {
        UsageAnalysis mergedUsageAnalysis = new UsageAnalysis();
        aUsageAnalysis.getAnalysis().keySet().forEach(clazz -> {
            Set<String> methods = aUsageAnalysis.getAnalysis().get(clazz);
            if (anotherUsageAnalysis.getAnalysis().containsKey(clazz)) {
                methods.addAll(anotherUsageAnalysis.getAnalysis().get(clazz));
            }
            mergedUsageAnalysis.getAnalysis().put(clazz, methods);
        });
        return mergedUsageAnalysis;
    }

    private void writeClassStatusPerDependency(final Set<String> usedClasses)
    {
        final String reportClassStatusPerDependencyFileName = "debloat-dependencies-report.csv";
        this.getLog().info("Writing " + reportClassStatusPerDependencyFileName + " to " +
            new File(getProject().getBasedir().getAbsolutePath() + "/"));
        StringBuilder s = new StringBuilder();
        Set<JarUtils.DependencyFileMapper> dependencyFileMappers = JarUtils.getDependencyFileMappers();
        for (JarUtils.DependencyFileMapper fileMapper : dependencyFileMappers) {
            for (final String dependencyJarName : fileMapper.getDependencyClassMap().keySet()) {
                s.append(dependencyJarName).append("\n");
                for (final String classInTheDependency : fileMapper.getDependencyClassMap().get(dependencyJarName)) {
                    if (usedClasses.contains(classInTheDependency)) {
                        s.append("\t" + "UsedClass, ").append(classInTheDependency).append("\n");
                    } else {
                        s.append("\t" + "BloatedClass, ").append(classInTheDependency).append("\n");
                    }
                }
            }
        }
        try {
            FileUtils.writeStringToFile(new File(getProject().getBasedir().getAbsolutePath() + "/" +
                reportClassStatusPerDependencyFileName), s.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.getLog().error("Error creating dependency bloat report file.");
        }
    }

    private void writeTimeElapsedReportFile(final Instant start)
    {
        this.getLog().info("Writing dependency-bloat-report.csv to " +
            new File(getProject().getBasedir().getAbsolutePath() + "/"));
        Instant finish = Instant.now();
        double timeElapsed = Duration.between(start, finish).toMillis();
        final String timeElapsedInSeconds = "Total debloat time: " + timeElapsed / 1000 + " s";
        this.getLog().info(timeElapsedInSeconds);
        try {
            final String reportExecutionTimeFileName = "debloat-execution-time.log";
            FileUtils.writeStringToFile(new File(getProject().getBasedir().getAbsolutePath() + "/" +
                reportExecutionTimeFileName), timeElapsedInSeconds, StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.getLog().error("Error creating time elapsed report file.");
        }
    }

    private void cleanReportFile()
    {
        try {
            FileUtils.writeStringToFile(new File(getProject().getBasedir().getAbsolutePath() + "/" +
                getReportFileName()), "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.getLog().error("Error cleaning report file.");
        }
    }

    private void printClassesLoaded()
    {
        final int nbOfClassesLoaded = ClassesLoadedSingleton.INSTANCE.getClassesLoaded().size();
        this.getLog().info("Loaded classes (" + nbOfClassesLoaded + ')');
        ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
        this.getLog().info(getLineSeparator());
    }

    private void removeUnusedMethods(final String outputDirectory, final UsageAnalysis usageAnalysis,
        Set<StackLine> failingMethods)
    {
        AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(outputDirectory,
            usageAnalysis,
            new File(getProject().getBasedir().getAbsolutePath() + "/" + getReportFileName()), failingMethods);
        try {
            testBasedMethodDebloat.removeUnusedMethods();
        } catch (IOException e) {
            this.getLog().error(String.format("Error: %s", e));
        }
    }

    private void removeUnusedClasses(final String outputDirectory, final Set<String> usedClasses)
    {
        try {
            JDblFileUtils jdblFileUtils = new JDblFileUtils(outputDirectory,
                new HashSet<>(),
                usedClasses,
                new File(getProject().getBasedir().getAbsolutePath() + "/" + getReportFileName()),
                getProject().getTestClasspathElements());
            jdblFileUtils.deleteUnusedClasses(outputDirectory);
        } catch (Exception e) {
            this.getLog().error(String.format("Error deleting unused classes: %s", e));
        }
    }

    private static Set<String> getUsedClasses(final UsageAnalysis usageAnalysis)
    {
        // get the union of the JaCoCo output and the JVM class loader results
        Set<String> usedClasses = new HashSet<>(ClassesLoadedSingleton.INSTANCE.getClassesLoaded());
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
