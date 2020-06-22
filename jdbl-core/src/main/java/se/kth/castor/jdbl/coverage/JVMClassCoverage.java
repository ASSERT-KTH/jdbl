package se.kth.castor.jdbl.coverage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import se.kth.castor.jdbl.util.ClassesLoadedSingleton;

public class JVMClassCoverage
{
    private static final Logger LOGGER = LogManager.getLogger(JVMClassCoverage.class.getName());


    public static void runTestsInVerboseMode() throws IOException
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
                // should not happen
            }
            input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            try {
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // print info about the number of classes loaded
        ClassesLoadedSingleton.INSTANCE.setClassesLoaded(classesLoadedTestDebloat);
    }
}
