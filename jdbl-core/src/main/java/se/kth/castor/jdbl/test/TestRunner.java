package se.kth.castor.jdbl.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;

import se.kth.castor.jdbl.coverage.YajtaCoverage;
import se.kth.castor.jdbl.util.MyFileWriter;

public class TestRunner
{
    private static final Logger LOGGER = LogManager.getLogger(YajtaCoverage.class.getName());

    public static void runTests(MavenProject mavenProject, boolean isTestRunForCoverage) throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        Process p;

        if (isTestRunForCoverage) {
            p = rt.exec("mvn surefire:test");
        } else {
            p = rt.exec("mvn test -Dmaven.main.skip=true -Drat.skip=true -Danimal.sniffer.skip=true -Dmaven.javadoc.skip=true -Dlicense.skip=true -Dsource.skip=true");
        }

        printProcessToStandardOutput(p);

        try {
            p.waitFor();
        } catch (
            InterruptedException e) {
            LOGGER.error("Re-testing process terminated unexpectedly.");
            Thread.currentThread().interrupt();
        }

        TestResultReader testResultReader = new TestResultReader(".");
        TestResult testResult = testResultReader.getResults();

        MyFileWriter myFileWriter = new MyFileWriter(mavenProject.getBasedir().getAbsolutePath());
        myFileWriter.writeTestResultsToFile(testResult);

        if (testResult.errorTests() != 0 || testResult.failedTests() != 0) {
            LOGGER.error("JDBL: THERE ARE TESTS FAILURES");
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
