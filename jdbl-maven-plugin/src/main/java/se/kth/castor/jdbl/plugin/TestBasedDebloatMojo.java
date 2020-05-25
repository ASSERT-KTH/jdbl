package se.kth.castor.jdbl.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.app.DebloatTypeEnum;
import se.kth.castor.jdbl.app.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.app.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.app.test.StackLine;
import se.kth.castor.jdbl.app.test.TestResult;
import se.kth.castor.jdbl.app.test.TestResultReader;
import se.kth.castor.jdbl.app.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.app.util.JDblFileUtils;
import se.kth.castor.jdbl.app.util.JarUtils;
import se.kth.castor.jdbl.app.wrapper.JacocoWrapper;

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
        printCustomStringToConsole("S T A R T I N G    T E S T    B A S E D    D E B L O A T");
        Instant start = Instant.now();
        cleanReportFile();
        String outputDirectory = getProject().getBuild().getOutputDirectory();

        // run JaCoCo usage analysis
        Map<String, Set<String>> jaCoCoUsageAnalysis = this.getJaCoCoUsageAnalysis();
        Set<String> usedClasses = null;
        try {
            this.printClassesLoaded();
            usedClasses = TestBasedDebloatMojo.getUsedClasses(jaCoCoUsageAnalysis);
        } catch (RuntimeException e) {
            this.getLog().error("Error computing JaCoCo usage analysis.");
        }

        TestResultReader testResultReader = new TestResultReader(".");
        Set<StackLine> failingMethods = testResultReader.getMethodFromStackTrace();
        for (StackLine failingMethod : failingMethods) {
            usedClasses.add(failingMethod.getClassName());
        }

        // write to a file with the status of classes (Used or Bloated) in each dependency
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
        this.removeUnusedMethods(outputDirectory, jaCoCoUsageAnalysis, failingMethods);

        // ----------------------------------------------------
        try {
            this.getLog().info("Starting running the test suite on the debloated version...");
            rerunTests();
        } catch (IOException e) {
            this.getLog().error("IOException when rerunning the tests");
        }

        // ----------------------------------------------------
        writeTimeElapsedReportFile(start);
        printCustomStringToConsole("T E S T S    B A S E D    D E B L O A T    F I N I S H E D");
    }

    private void rerunTests() throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("mvn test -Dmaven.main.skip=true");

        printProcessToStandardOutput(p);

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            this.getLog().error("Re-testing process terminated unexpectedly.");
            Thread.currentThread().interrupt();
        }
        TestResultReader testResultReader = new TestResultReader(".");
        TestResult testResult = testResultReader.getResults();
        writeTSResultsToFile(testResult);
        if (testResult.errorTests() != 0 || testResult.failedTests() != 0) {
            printCustomStringToConsole("T E S T S    B A S E D    D E B L O A T    F A I L E D");
        }
    }

    private void printProcessToStandardOutput(final Process p) throws IOException
    {
        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        input.close();
    }

    private void writeTSResultsToFile(final TestResult testResult)
    {
        this.getLog().info(testResult.getResults());
        this.getLog().info("Writing ts-results.log to " +
            new File(getProject().getBasedir().getAbsolutePath() + "/" + "ts-results.log"));
        try {
            final String reportTSResultsFileName = "ts-results.log";
            FileUtils.writeStringToFile(new File(getProject().getBasedir().getAbsolutePath() + "/" +
                reportTSResultsFileName), testResult.getResults(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.getLog().error("Error creating tests results report file.");
        }
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
        ClassesLoadedSingleton.INSTANCE.getClassesLoaded().stream().forEach(System.out::println);
        this.getLog().info(getLineSeparator());
    }

    private void removeUnusedMethods(final String outputDirectory, final Map<String, Set<String>> usageAnalysis, Set<StackLine> failingMethods)
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

    private static Set<String> getUsedClasses(final Map<String, Set<String>> usageAnalysis)
    {
        // get the union of the JaCoCo output and the JVM class loader results
        Set<String> usedClasses = new HashSet<>(ClassesLoadedSingleton.INSTANCE.getClassesLoaded());
        usageAnalysis
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .forEach(className -> usedClasses.add(className.getKey().replace('/', '.')));
        return usedClasses;
    }

    private Map<String, Set<String>> getJaCoCoUsageAnalysis()
    {
        JacocoWrapper jacocoWrapper = new JacocoWrapper(getProject(),
            new File(getProject().getBasedir().getAbsolutePath() + "/target/report.xml"),
            DebloatTypeEnum.TEST_DEBLOAT);
        Map<String, Set<String>> usageAnalysis = null;
        try {
            usageAnalysis = jacocoWrapper.analyzeUsages();
            this.printJaCoCoUsageAnalysisResults(usageAnalysis);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            this.getLog().error(e);
        }
        return usageAnalysis;
    }

    private void printJaCoCoUsageAnalysisResults(final Map<String, Set<String>> usageAnalysis)
    {
        this.getLog().info("JaCoCo ANALYSIS RESULTS:");
        this.getLog().info(String.format("Total unused classes: %d",
            usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count()));
        this.getLog().info(String.format("Total unused methods: %d",
            usageAnalysis.values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));
        this.getLog().info(getLineSeparator());
    }
}
