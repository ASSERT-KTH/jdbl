package se.kth.castor.jdbl.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;

import se.kth.castor.jdbl.coverage.YajtaCoverage;

public class TestRunner
{
    private static final Logger LOGGER = LogManager.getLogger(YajtaCoverage.class.getName());

    public static void runTests(MavenProject mavenProject) throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("mvn test -Dmaven.main.skip=true -Drat.skip=true -Danimal.sniffer.skip=true -Dmaven.javadoc.skip=true -Dlicense.skip=true -Dsource.skip=true");

        printProcessToStandardOutput(p);

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            LOGGER.error("Re-testing process terminated unexpectedly.");
            Thread.currentThread().interrupt();
        }
        TestResultReader testResultReader = new TestResultReader(".");
        TestResult testResult = testResultReader.getResults();
        writeTSResultsToFile(testResult, mavenProject);
        if (testResult.errorTests() != 0 || testResult.failedTests() != 0) {
            LOGGER.error("JDBL: THERE ARE TESTS FAILURES");
        }
    }

    private static void writeTSResultsToFile(final TestResult testResult, MavenProject mavenProject)
    {
        LOGGER.info(testResult.getResults());
        LOGGER.info("Writing ts-results.log to " +
            new File(mavenProject.getBasedir().getAbsolutePath() + "/" + "ts-results.log"));
        try {
            final String reportTSResultsFileName = "ts-results.log";
            FileUtils.writeStringToFile(new File(mavenProject.getBasedir().getAbsolutePath() + "/" +
                reportTSResultsFileName), testResult.getResults(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error creating tests results report file.");
        }
    }

    private static void printProcessToStandardOutput(final Process p) throws IOException
    {
        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        input.close();
    }
}
