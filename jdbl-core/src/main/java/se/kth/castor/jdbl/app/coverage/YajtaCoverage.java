package se.kth.castor.jdbl.app.coverage;

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
import se.kth.castor.jdbl.app.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.app.test.TestRunner;
import se.kth.castor.jdbl.app.util.JarUtils;
import se.kth.castor.jdbl.app.util.MavenUtils;
import se.kth.castor.offline.CoverageInstrumenter;
import se.kth.castor.yajta.api.MalformedTrackingClassException;

public class YajtaCoverage extends CoverageWrapper implements UsageAnalyzer
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
        // yajta could produce more than one coverage file (in case of parallel testing), so we need to read all of them
        for (String fileName : filesInBasedir) {
            if (fileName.startsWith("yajta_coverage")) {
                String json;
                try {
                    json = new String(Files.readAllBytes(Paths.get(projectBasedir +
                        "/" + fileName)), StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    // convert JSON string to Map
                    Map<String, ArrayList<String>> map = mapper.readValue(json, Map.class);
                    Iterator it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        // add the yajta coverage results to the jacoco analysis
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

        // copy dependencies to be instrumented by yajta
        MavenUtils mavenUtils = new MavenUtils(super.mavenHome, mavenProject.getBasedir());
        mavenUtils.copyDependencies(classesDir);
        JarUtils.decompressJars(classesDir);

        // TODO Delete non class files (this should not happen in a new version of yajta)
        File directory = new File(classesDir + "/META-INF");
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Error deleting directory " + directory.getName());
        }

        try {
            CoverageInstrumenter.main(new String[]{
                "-i", classesDir,
                "-o", instrumentedDir});
        } catch (MalformedTrackingClassException e) {
            LOGGER.error("Error executing yajta.");
        }
        try {
            FileUtils.moveDirectory(new File(classesDir),
                new File(classesOriginalDir));
            FileUtils.moveDirectory(new File(instrumentedDir),
                new File(classesDir));
        } catch (IOException e) {
            LOGGER.error("Error handling target/class directory.");
        }
        try {
            mavenUtils.copyDependency("se.kth.castor:yajta-core:2.0.2", testDir);
            mavenUtils.copyDependency("se.kth.castor:yajta-offline:2.0.2", testDir);
            JarUtils.decompressJars(testDir);
            TestRunner.runTests(mavenProject);
        } catch (IOException e) {
            LOGGER.error("Error rerunning the tests.");
        }
        try {
            FileUtils.deleteDirectory(new File(classesDir));
            FileUtils.moveDirectory(new File(classesOriginalDir),
                new File(classesDir));
        } catch (IOException e) {
            LOGGER.error("Error rolling back the compiled classes.");
        }
    }

    private Set<String> listFilesInDirectory(String dir)
    {
        return Stream.of(new File(dir).listFiles())
            .filter(file -> !file.isDirectory())
            .map(File::getName)
            .collect(Collectors.toSet());
    }
}
