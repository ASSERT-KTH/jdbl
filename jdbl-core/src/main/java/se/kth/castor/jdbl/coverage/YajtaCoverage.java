package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;
import se.kth.castor.offline.CoverageInstrumenter;
import se.kth.castor.yajta.api.MalformedTrackingClassException;

public class YajtaCoverage extends AbstractCoverage implements UsageAnalyzer
{
    private static final Logger LOGGER = LogManager.getLogger(YajtaCoverage.class.getName());

    public YajtaCoverage(
        MavenProject mavenProject,
        File mavenHome,
        DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
    }

    @Override
    public UsageAnalysis analyzeUsages()
    {
        writeCoverage();
        UsageAnalysis usageAnalysis = new UsageAnalysis();
        final String projectBasedir = mavenProject.getBasedir().getAbsolutePath();
        Set<String> filesInBasedir = listFilesInDirectory(projectBasedir);
        // Yajta could produce more than one coverage file (in case of parallel testing), so we need to read all of them
        for (String fileName : filesInBasedir) {
            if (fileName.startsWith("yajta_coverage")) {
                String json;
                try {
                    json = new String(Files.readAllBytes(Paths.get(projectBasedir +
                        "/" + fileName)), StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    // Convert JSON string to Map
                    Map<String, ArrayList<String>> map = mapper.readValue(json, Map.class);
                    Iterator it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        // Add the yajta coverage results to the jacoco analysis
                        final String className = String.valueOf(pair.getKey()).replace(".", "/");
                        ArrayList<String> yajtaMethods = map.get(pair.getKey());
                        usageAnalysis.getAnalysis().put(className, new HashSet<>(yajtaMethods));
                    }
                } catch (IOException e) {
                    LOGGER.error("Error reading the yajta coverage file.");
                }
            }
        }
        return usageAnalysis;
    }

    public void writeCoverage()
    {
        LOGGER.info("Running yajta");
        final String classesDir = mavenProject.getBasedir().getAbsolutePath() + "/target/classes";
        final String testDir = mavenProject.getBasedir().getAbsolutePath() + "/target/test-classes";
        final String instrumentedDir = mavenProject.getBasedir().getAbsolutePath() + "/target/instrumented";
        final String classesOriginalDir = mavenProject.getBasedir().getAbsolutePath() + "/target/classes-original";
        MavenUtils mavenUtils = copyDependencies(classesDir);
        // deleteNonClassFiles(classesDir);
        instrument(classesDir, instrumentedDir);
        replaceInstrumentedClasses(classesDir, instrumentedDir, classesOriginalDir);
        addYajtaAsTestDependency(testDir, mavenUtils);
        restoreOriginalClasses(classesDir, classesOriginalDir);
    }

    /**
     * Restore the (previously replaced) original classes with the original non-instrumented classes.
     */
    private void restoreOriginalClasses(final String classesDir, final String classesOriginalDir)
    {
        try {
            FileUtils.deleteDirectory(new File(classesDir));
            FileUtils.moveDirectory(new File(classesOriginalDir), new File(classesDir));
        } catch (IOException e) {
            LOGGER.error("Error rolling back the compiled classes.");
        }
    }

    /**
     * The instrumented classes need yajta to compile with the inserted probes.
     */
    private void addYajtaAsTestDependency(final String testDir, final MavenUtils mavenUtils)
    {
        try {
            mavenUtils.copyDependency("se.kth.castor:yajta-core:2.0.2", testDir);
            mavenUtils.copyDependency("se.kth.castor:yajta-offline:2.0.2", testDir);
            JarUtils.decompressJars(testDir);
            TestRunner.runTests(mavenProject);
        } catch (IOException e) {
            LOGGER.error("Error rerunning the tests.");
        }
    }

    /**
     * Replace the original compiled classes with the instrumented classes.
     */
    private void replaceInstrumentedClasses(String classesDir, String instrumentedDir, String classesOriginalDir)
    {
        LOGGER.info("Moving classes");
        LOGGER.info(classesDir);
        LOGGER.info(instrumentedDir);
        LOGGER.info(classesOriginalDir);
        try {
            FileUtils.moveDirectory(new File(classesDir),
                new File(classesOriginalDir));
            FileUtils.moveDirectory(new File(instrumentedDir),
                new File(classesDir));
        } catch (IOException e) {
            LOGGER.error("Error handling target/class directory.");
        }
    }

    /**
     * Instrument classes with yajta.
     */
    private void instrument(final String classesDir, final String instrumentedDir)
    {
        try {
            CoverageInstrumenter.main(new String[]{
                "-i", classesDir,
                "-o", instrumentedDir});
        } catch (MalformedTrackingClassException e) {
            LOGGER.error("Error executing yajta.");
        }
    }

    /**
     * Delete non class files to avoid wrong instrumentation attempts (e.g., resources).
     */
    private void deleteNonClassFiles(final String classesDir)
    {
        File directory = new File(classesDir + "/META-INF");
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Error deleting directory " + directory.getName());
        }
    }

    /**
     * Copy dependencies to be instrumented by yajta
     */
    private MavenUtils copyDependencies(final String classesDir)
    {
        MavenUtils mavenUtils = new MavenUtils(super.mavenHome, mavenProject.getBasedir());
        mavenUtils.copyDependencies(classesDir);
        JarUtils.decompressJars(classesDir);
        return mavenUtils;
    }

    /**
     * Recursively retrieve the absolute paths of al the files in a directory.
     */
    private Set<String> listFilesInDirectory(String dir)
    {
        return Stream.of(new File(dir).listFiles())
            .filter(file -> !file.isDirectory())
            .map(File::getName)
            .collect(Collectors.toSet());
    }
}
