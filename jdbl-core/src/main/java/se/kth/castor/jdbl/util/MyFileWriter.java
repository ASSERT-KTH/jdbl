package se.kth.castor.jdbl.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import se.kth.castor.jdbl.coverage.CoverageToolEnum;
import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.coverage.UsageStatusEnum;
import se.kth.castor.jdbl.test.TestResult;

public class MyFileWriter
{
    private static final Logger LOGGER = LogManager.getLogger(MyFileWriter.class.getName());

    public static final String DEBLOAT_EXECUTION_TIME_FILE_NAME = "debloat-execution-time.log";
    public static final String DEBLOAT_DEPENDENCIES_REPORT_FILE_NAME = "debloat-dependencies-report.csv";
    public static final String DEBLOAT_REPORT_FILE_NAME = "debloat-report.csv";
    public static final String TS_RESULTS_LOG_FILE_NAME = "ts-results.log";
    public static final String COVERAGE_RESULTS_FILE_NAME = "coverage-results.csv";
    public static final String DEPENDENCY_TREE_FILE_NAME = "dependency-tree.txt";
    private static final String SKIPPED_CLASSES_REPORT_FILE = "preserved-classes.csv";

    private final String projectBaseDir;
    private final String reportsBaseDir;

    public MyFileWriter(final String projectBaseDir)
    {
        this.projectBaseDir = projectBaseDir;
        this.reportsBaseDir = projectBaseDir + "/.jdbl/";
    }

    /**
     * Write the coverage results, i.e., classes and methods covered, of a coverage tool.
     */
    public void writeCoverageAnalysisToFile(CoverageToolEnum coverageTool, UsageAnalysis usageAnalysis)
    {
        LOGGER.info("Writing coverage results of " + coverageTool.getName() + " to file " + COVERAGE_RESULTS_FILE_NAME +
            " in " + reportsBaseDir);
        StringBuilder sb = new StringBuilder();
        Set<String> classes = usageAnalysis.classes();
        for (String clazz : classes) {
            Set<String> methods = usageAnalysis.methods(clazz);
            for (String method : methods) {
                sb.append(coverageTool.getName())
                    .append(",")
                    .append(clazz.replace("/", "."))
                    .append(",")
                    .append(method)
                    .append("\n");
            }
        }
        try {
            FileUtils.writeStringToFile(new File(reportsBaseDir + COVERAGE_RESULTS_FILE_NAME),
                sb.toString(), StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            LOGGER.error("Error creating the coverage results report file.");
        }
    }

    /**
     * Writhe the dependency tree of the project to file in the root of the project.
     */
    public void writeDependencyTreeToFile()
    {
        LOGGER.info("Writing " + DEPENDENCY_TREE_FILE_NAME + " in " + reportsBaseDir);
        MavenUtils mavenUtils = new MavenUtils(new File(System.getenv().get("M2_HOME")), new File(projectBaseDir));
        mavenUtils.dependencyTree(reportsBaseDir + DEPENDENCY_TREE_FILE_NAME);
    }

    /**
     * Write the status of each class per dependency. We need to exclude JaCoCo and Yajta, since they were included as
     * dependencies by JDBL, and are not part of the dependencies of the project.
     */
    public void writeClassStatusPerDependency(final Set<String> classes)
    {
        LOGGER.info("Writing " + DEBLOAT_DEPENDENCIES_REPORT_FILE_NAME + " in " + reportsBaseDir);
        StringBuilder s = new StringBuilder();
        Set<JarUtils.DependencyFileMapper> dependencyFileMappers = JarUtils.getDependencyFileMappers();
        for (JarUtils.DependencyFileMapper fileMapper : dependencyFileMappers) {
            for (final String dependencyJarName : fileMapper.getDependencyClassMap().keySet()) {
                // Exclude JaCoCo and Yajta from the dependencies' report
                if (dependencyJarName.startsWith("jacocoagent") ||
                    dependencyJarName.startsWith("yajta") ||
                    dependencyJarName.startsWith("org.jacoco.agent")) {
                    continue;
                }
                analyzeClassesInDependency(classes, s, fileMapper, dependencyJarName);
            }
        }
        try {
            FileUtils.writeStringToFile(new File(reportsBaseDir +
                DEBLOAT_DEPENDENCIES_REPORT_FILE_NAME), s.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating dependency bloat report file.");
        }
    }

    private void analyzeClassesInDependency(Set<String> classes, StringBuilder s,
        JarUtils.DependencyFileMapper fileMapper, String dependencyJarName)
    {
        for (final String classInTheDependency : fileMapper.getDependencyClassMap().get(dependencyJarName)) {
            if (classes.contains(classInTheDependency)) {
                s.append(dependencyJarName, 0, dependencyJarName.length() - 4)
                    .append(",")
                    .append(UsageStatusEnum.USED_CLASS.getName())
                    .append(",")
                    .append(classInTheDependency)
                    .append("\n");
            } else {
                s.append(dependencyJarName, 0, dependencyJarName.length() - 4)
                    .append(",")
                    .append(UsageStatusEnum.BLOATED_CLASS.getName())
                    .append(",")
                    .append(classInTheDependency)
                    .append("\n");
            }
        }
    }

    /**
     * Write the execution time, in seconds, of JDBL to a file.
     */
    public void writeTimeElapsedReportFile(final Instant start)
    {
        LOGGER.info("Writing " + DEBLOAT_EXECUTION_TIME_FILE_NAME + " in " + reportsBaseDir);
        Instant finish = Instant.now();
        double timeElapsed = Duration.between(start, finish).toMillis();
        final String timeElapsedInSeconds = "Total debloat time: " + timeElapsed / 1000 + " s";
        LOGGER.info(timeElapsedInSeconds);
        try {
            FileUtils.writeStringToFile(new File(reportsBaseDir +
                DEBLOAT_EXECUTION_TIME_FILE_NAME), timeElapsedInSeconds, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating time elapsed report file.");
        }
    }

    /**
     * Write the debloat report.
     */
    public void writeDebloatReport(String usageStatus, String currentClassName, ClassFileType classFileType)
    {
        File reportFile = new File(reportsBaseDir + DEBLOAT_REPORT_FILE_NAME);
        try {
            FileUtils.writeStringToFile(reportFile, usageStatus + "," +
                currentClassName + "," +
                classFileType.toString() +
                "\n", StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            LOGGER.error("Error writing the debloat report.");
        }
    }

    /**
     * Write the results of the tests, as per the surefire standard output.
     * Example: Tests run: 25, Failures: 0, Errors: 0, Skipped: 0.
     */
    public void writeTestResultsToFile(final TestResult testResult)
    {
        LOGGER.info(testResult.getResults());
        LOGGER.info("Writing ts-results.log to " + new File(reportsBaseDir + TS_RESULTS_LOG_FILE_NAME));
        try {
            FileUtils.writeStringToFile(new File(reportsBaseDir + TS_RESULTS_LOG_FILE_NAME),
                testResult.getResults(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating tests results report file.");
        }
    }

    /**
     * Write the classes that where skipped (i.e., not debloated) during the debloat process.
     */
    public void writePreservedClass(final String currentClassName, final ClassFileType classFileType)
    {
        try {
            final String lineToWrite = currentClassName + "," + classFileType.name() + "\n";
            FileUtils.writeStringToFile(new File(reportsBaseDir + SKIPPED_CLASSES_REPORT_FILE),
                lineToWrite, StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            LOGGER.error("Error creating tests results report file.");
        }
    }

    /**
     * Remove the directory with all the JDBL reports.
     * This methods is useful at the beginning of the execution to clean up the reports files.
     */
    public void resetJDBLReportsDirectory()
    {
        final File jdblReportsDirectory = new File(reportsBaseDir);
        if (jdblReportsDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(jdblReportsDirectory);
            } catch (IOException e) {
                LOGGER.error("Error while resetting the JDBL reports directory");
            }
        }
    }
}
