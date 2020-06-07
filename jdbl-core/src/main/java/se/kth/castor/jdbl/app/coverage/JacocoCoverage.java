package se.kth.castor.jdbl.app.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.app.adapter.ConstantAdapter;
import se.kth.castor.jdbl.app.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.app.deptree.OptionalDependencyIgnorer;
import se.kth.castor.jdbl.app.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.app.util.CmdExec;
import se.kth.castor.jdbl.app.util.JarUtils;
import se.kth.castor.jdbl.app.util.MavenUtils;

public class JacocoCoverage extends CoverageWrapper implements UsageAnalyzer
{
    private static final Logger LOGGER = LogManager.getLogger(JacocoCoverage.class.getName());

    private List<String> tests;
    private File report;

    public JacocoCoverage(
        MavenProject mavenProject,
        File mavenHome,
        File report,
        DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
        this.report = report;
        this.tests = new ArrayList<>();
        if (report.exists()) {
            FileUtils.deleteQuietly(report);
        }
    }

    public JacocoCoverage(
        MavenProject mavenProject,
        File report,
        DebloatTypeEnum debloatTypeEnum,
        String entryClass,
        String entryMethod,
        String entryParameters,
        File mavenHome)
    {
        super(mavenProject, mavenHome, debloatTypeEnum, entryClass, entryMethod, entryParameters);
        this.report = report;
        this.entryClass = entryClass;
        this.entryMethod = entryMethod;
        this.entryParameters = entryParameters;
        this.mavenHome = mavenHome;
        if (report.exists()) {
            FileUtils.deleteQuietly(report);
        }
    }

    @Override
    public UsageAnalysis analyzeUsages()
    {
        final String classesDir = mavenProject.getBasedir().getAbsolutePath() + "/target/classes";
        final String testClasspath = mavenProject.getBasedir().getAbsolutePath() + "/target/test-classpath";

        MavenUtils mavenUtils = new MavenUtils(this.mavenHome, mavenProject.getBasedir());

        // Write all the test classpath to a local file
        Properties propertyTestClasspath = new Properties();
        propertyTestClasspath.setProperty("mdep.outputFile", testClasspath);
        propertyTestClasspath.setProperty("scope", "test");
        mavenUtils.runMaven(Collections.singletonList("dependency:build-classpath"), propertyTestClasspath);

        // Do not copy the dependencies with non-compile scopes
        Properties propertyCopyDependencies = new Properties();
        propertyCopyDependencies.setProperty("outputDirectory", classesDir);
        propertyCopyDependencies.setProperty("includeScope", "runtime");
        propertyCopyDependencies.setProperty("exclude", "runtime");
        mavenUtils.runMaven(Collections.singletonList("dependency:copy-dependencies"), propertyCopyDependencies);

        // Do not process the optional dependencies
        OptionalDependencyIgnorer optionalDependencyIgnorer = new OptionalDependencyIgnorer(mavenProject);
        optionalDependencyIgnorer.removeOptionalDependencies(mavenUtils);

        // Decompress the dependencies
        JarUtils.decompressJars(classesDir);

        // Apply bytecode transformations
        Iterator<File> itFiles = FileUtils.iterateFiles(new File(classesDir), new String[]{"class"}, true);
        while (itFiles.hasNext()) {
            File file = itFiles.next();
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                ConstantAdapter constantAdapter = new ConstantAdapter(fileInputStream);
                byte[] result = constantAdapter.addField();
                FileUtils.forceDelete(file);
                FileUtils.writeByteArrayToFile(new File(file.getAbsolutePath()), result);
                fileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Error applying bytecode transformation.");
            }
        }

        // Instrument the code
        mavenUtils.runMaven(Collections.singletonList("org.jacoco:jacoco-maven-plugin:0.8.4:instrument"), null);

        System.exit(1);

        switch (this.debloatTypeEnum) {
            case TEST_DEBLOAT:
                try {
                    this.testBasedDebloat();
                } catch (Exception e) {
                    LOGGER.error("Error during JaCoCo test-based debloat.");
                }
                break;
            case ENTRY_POINT_DEBLOAT:
                try {
                    this.entryPointDebloat();
                } catch (Exception e) {
                    LOGGER.error("Error during JaCoCo entry-point debloat");
                }
                break;
            case CONSERVATIVE_DEBLOAT:
                // TODO implement the conservative approach
                break;
        }

        // Restore instrumented classes and generate the jacoco xml report
        mavenUtils.runMaven(Arrays.asList(
            "org.jacoco:jacoco-maven-plugin:0.8.4:restore-instrumented-classes",
            "org.jacoco:jacoco-maven-plugin:0.8.4:report"), null);

        // Copy the jacoco xml report
        try {
            FileUtils.copyFile(new File(mavenProject.getBasedir().getAbsolutePath() + "/target/site/jacoco/jacoco.xml"),
                report);
        } catch (IOException e) {
            LOGGER.error("Error copying jacoco.xml file.");
        }

        // Read the jacoco report
        JacocoReportReader reportReader = null;
        try {
            reportReader = new JacocoReportReader();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Error parsing jacoco.xml file.");
        }

        try {
            assert reportReader != null;
            return reportReader.getUsedClassesAndMethods(report);
        } catch (IOException | SAXException e) {
            LOGGER.error("Error getting unused classes and methods file.");
        }
        return null;
    }

    private void testBasedDebloat() throws IOException
    {
        Set<String> classesLoadedTestDebloat = new HashSet<>();
        FileUtils.deleteDirectory(new File("target/classes"));
        // String agentParameter = "-javaagent:" + new File("coverageAgent.jar").getAbsoluteFile();
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
                    if (line.contains("class,load") && line.endsWith("target/classes/")) {
                        classesLoadedTestDebloat.add(line.split(" ")[1]);
                    } else if (line.contains("[Loaded") && line.endsWith("target/classes/]")) {
                        classesLoadedTestDebloat.add(line.split(" ")[1]);
                    }
                }
            } catch (IOException e) {
                // should not happen
                LOGGER.error(e);
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
            LOGGER.error("Error getting the loaded classes after executing the test cases.");
            Thread.currentThread().interrupt();
        }

        // print info about the number of classes loaded
        ClassesLoadedSingleton.INSTANCE.setClassesLoaded(classesLoadedTestDebloat);
        // print the list of classes loaded
        ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
    }

    private void entryPointDebloat() throws IOException
    {
        LOGGER.info("Output directory: " + mavenProject.getBuild().getOutputDirectory());
        LOGGER.info("entryClass: " + entryClass);
        LOGGER.info("entryParameters: " + entryParameters);
        // add jacoco to the classpath
        String classpath = addJacocoToClasspath(mavenProject.getBasedir().getAbsolutePath() + "/target/test-classpath");
        // execute the application from entry point
        CmdExec cmdExecEntryPoint = new CmdExec();
        Set<String> classesLoaded = cmdExecEntryPoint.execProcess(classpath, entryClass, entryParameters.split(" "));
        // print info about the number of classes loaded
        LOGGER.info("Number of classes loaded: " + classesLoaded.size());
        ClassesLoadedSingleton.INSTANCE.setClassesLoaded(classesLoaded);
    }

    private String addJacocoToClasspath(String file) throws IOException
    {
        StringBuilder rawFile;
        try (BufferedReader buffer = new BufferedReader(new FileReader(file))) {
            rawFile = new StringBuilder(mavenProject.getBasedir().getAbsolutePath() + "/target/classes/:");
            String line;
            while ((line = buffer.readLine()) != null) {
                rawFile.append(line);
            }
        }
        return rawFile.toString();
    }

    private List<String> findTestFiles(String testOutputDirectory)
    {
        File file = new File(testOutputDirectory);
        File[] list = file.listFiles();
        assert list != null;
        for (File testFile : list) {
            if (testFile.isDirectory()) {
                this.findTestFiles(testFile.getAbsolutePath());
            } else if (testFile.getName().endsWith(".class")) {
                String testName = testFile.getAbsolutePath();
                // Get the binary name of the test file
                tests.add(testName.replaceAll("/", ".")
                    .substring(mavenProject.getBuild().getTestOutputDirectory().length() + 1, testName.length() - 6));
            }
        }
        return tests;
    }
}
