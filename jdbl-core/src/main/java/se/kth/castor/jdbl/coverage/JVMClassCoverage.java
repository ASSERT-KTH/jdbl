package se.kth.castor.jdbl.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.maven.project.MavenProject;

import se.kth.castor.jdbl.debloat.DebloatTypeEnum;

public class JVMClassCoverage extends AbstractCoverage
{
    public JVMClassCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
        LOGGER = LogManager.getLogger(JVMClassCoverage.class.getName());
    }

    protected UsageAnalysis executeTestBasedAnalysis()
    {
        try {
            writeCoverage();
        } catch (IOException e) {
            LOGGER.info("Error writing JVM usage analysis.");
        }
        UsageAnalysis usageAnalysis = new UsageAnalysis();
        Set<String> classesLoaded = JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded();
        for (String classLoaded : classesLoaded) {
            usageAnalysis.addEntry(classLoaded, new HashSet<>(Arrays.asList("UNKNOWN")));
        }

        return usageAnalysis;
    }

    public void writeCoverage() throws IOException
    {
        LOGGER.info("Starting executing tests in verbose mode to get JVM class loader report.");
        Set<String> classesLoadedTestDebloat = new HashSet<>();
        List<String> args = new ArrayList<>();
        args.add("mvn");
        args.add("test");
        args.add("-X");
        args.add("-DargLine=-verbose:class");
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Map<String, String> environment = processBuilder.environment();
        environment.put("JAVA_HOME", System.getenv().get("JAVA_HOME"));
        Process p = processBuilder.start();
        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            try {
                while ((line = input.readLine()) != null) {
                    if ((line.contains("class,load") && line.endsWith("target/classes/")) ||
                        (line.contains("[Loaded") && line.endsWith("target/classes/]"))) {
                        classesLoadedTestDebloat.add(line.split(" ")[1]);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error parsing line.");
            }
            input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            try {
                while ((line = input.readLine()) != null) {
                    LOGGER.info(line);
                }
            } catch (IOException e) {
                LOGGER.error("Error reading line.");
            }
        }).start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // print info about the number of classes loaded
        JVMClassesCoveredSingleton.INSTANCE.setClassesLoaded(classesLoadedTestDebloat);
    }

    protected UsageAnalysis executeConservativeAnalysis()
    {
        // TODO implement the conservative approach
        return null;
    }

    protected UsageAnalysis executeEntryPointAnalysis()
    {
        // TODO implement the entry point approach
        LOGGER.info("Output directory: " + mavenProject.getBuild().getOutputDirectory());
        LOGGER.info("entryClass: " + entryClass);
        LOGGER.info("entryMethod: " + entryMethod);
        LOGGER.info("entryParameters: " + entryParameters);
        return null;
    }
}
