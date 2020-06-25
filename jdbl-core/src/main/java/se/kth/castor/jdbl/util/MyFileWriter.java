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

import se.kth.castor.jdbl.coverage.UsageStatusEnum;
import se.kth.castor.jdbl.test.TestResult;

public class MyFileWriter
{
    private static final Logger LOGGER = LogManager.getLogger(MyFileWriter.class.getName());

    public static final String DEBLOAT_EXECUTION_TIME_FILE_NAME = "debloat-execution-time.log";
    public static final String DEBLOAT_DEPENDENCIES_REPORT_FILE_NAME = "debloat-dependencies-report.csv";
    public static final String DEBLOAT_REPORT_FILE_NAME = "debloat-report.csv";
    public static final String TS_RESULTS_LOG = "ts-results.log";

    private String projectBaseDir;

    public MyFileWriter(final String projectBaseDir)
    {
        this.projectBaseDir = projectBaseDir;
    }

    /**
     * Write the status of each class per dependency. We need to exclude Jacoco and yajta, since they were included as
     * dependencies by JDBL, and are not part of the dependencies of the project.
     */
    public void writeClassStatusPerDependency(final Set<String> classes)
    {
        LOGGER.info("Writing " + DEBLOAT_DEPENDENCIES_REPORT_FILE_NAME + " to " + new File(projectBaseDir + "/"));
        StringBuilder s = new StringBuilder();
        Set<JarUtils.DependencyFileMapper> dependencyFileMappers = JarUtils.getDependencyFileMappers();
        for (JarUtils.DependencyFileMapper fileMapper : dependencyFileMappers) {
            for (final String dependencyJarName : fileMapper.getDependencyClassMap().keySet()) {
                // Exclude JaCoCo and Yajta from the dependencies' report
                if (dependencyJarName.startsWith("jacocoagent") || dependencyJarName.startsWith("yajta") ||
                    dependencyJarName.startsWith("org.jacoco.agent")) {
                    continue;
                }
                analyzeClassesInDependency(classes, s, fileMapper, dependencyJarName);
            }
        }
        try {
            FileUtils.writeStringToFile(new File(projectBaseDir + "/" +
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
        LOGGER.info("Writing " + DEBLOAT_EXECUTION_TIME_FILE_NAME + " to " +
            new File(projectBaseDir + "/"));
        Instant finish = Instant.now();
        double timeElapsed = Duration.between(start, finish).toMillis();
        final String timeElapsedInSeconds = "Total debloat time: " + timeElapsed / 1000 + " s";
        LOGGER.info(timeElapsedInSeconds);
        try {
            FileUtils.writeStringToFile(new File(projectBaseDir + "/" +
                DEBLOAT_EXECUTION_TIME_FILE_NAME), timeElapsedInSeconds, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating time elapsed report file.");
        }
    }

    /**
     * Write the debloat report.
     */
    public void writeDebloatReport(String usageStatus, String currentClassName, FileType fileType)
    {
        File reportFile = new File(projectBaseDir + "/" + DEBLOAT_REPORT_FILE_NAME);
        try {
            FileUtils.writeStringToFile(reportFile, usageStatus + "," +
                currentClassName + "," +
                fileType.toString() +
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
        LOGGER.info("Writing ts-results.log to " + new File(projectBaseDir + "/" + TS_RESULTS_LOG));
        try {
            FileUtils.writeStringToFile(new File(projectBaseDir + "/" + TS_RESULTS_LOG),
                testResult.getResults(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating tests results report file.");
        }
    }
}
