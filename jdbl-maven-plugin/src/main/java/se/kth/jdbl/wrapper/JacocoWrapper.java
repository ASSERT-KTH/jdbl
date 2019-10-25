package se.kth.jdbl.wrapper;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;
import se.kth.jdbl.util.ClassesLoadedSingleton;
import se.kth.jdbl.util.CmdExec;
import se.kth.jdbl.util.MavenUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class JacocoWrapper {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private static final Logger LOGGER = LogManager.getLogger(JacocoWrapper.class.getName());
    private MavenProject mavenProject;
    private String entryClass;
    private String entryMethod;
    private String entryParameters;
    private List<String> tests;
    private File mavenHome;
    private File report;
    private DebloatTypeEnum debloatTypeEnum;

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public JacocoWrapper(MavenProject mavenProject, File report, DebloatTypeEnum debloatTypeEnum) {
        this.mavenProject = mavenProject;
        this.report = report;
        this.debloatTypeEnum = debloatTypeEnum;
        this.tests = new ArrayList<>();
        if (report.exists()) {
            FileUtils.deleteQuietly(report);
        }
    }

    public JacocoWrapper(MavenProject mavenProject, File report, DebloatTypeEnum debloatTypeEnum, String entryClass, String entryMethod, String entryParameters, File mavenHome) {
        this.mavenProject = mavenProject;
        this.report = report;
        this.debloatTypeEnum = debloatTypeEnum;
        this.entryClass = entryClass;
        this.entryMethod = entryMethod;
        this.entryParameters = entryParameters;
        this.mavenHome = mavenHome;
        if (report.exists()) {
            FileUtils.deleteQuietly(report);
        }
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    public Map<String, Set<String>> analyzeUsages() throws IOException, ParserConfigurationException, SAXException {
        MavenUtils mavenUtils = new MavenUtils(mavenHome, mavenProject.getBasedir());

        Properties propertyTestClasspath = new Properties();
        propertyTestClasspath.setProperty("mdep.outputFile", mavenProject.getBasedir().getAbsolutePath() + "/target/test-classpath");
        propertyTestClasspath.setProperty("scope", "test");

        // write all the test classpath to file locally
        mavenUtils.runMaven(Collections.singletonList("dependency:build-classpath"), propertyTestClasspath);

//        Properties propertyCopyDependencies = new Properties();
//        propertyCopyDependencies.setProperty("outputDirectory", baseDir.getAbsolutePath() + "/target/classes");
//        propertyCopyDependencies.setProperty("includeScope", "compile");
//        mavenUtils.runMaven(Collections.singletonList("dependency:copy-dependencies"), propertyCopyDependencies );
//        JarUtils.decompressJars(baseDir.getAbsolutePath() + "/target/classes");

        // instrument the code
        mavenUtils.runMaven(Collections.singletonList("org.jacoco:jacoco-maven-plugin:0.8.4:instrument"), null);

        switch (debloatTypeEnum) {
            case TEST_DEBLOAT:
                // add jacoco to the classpath
                String classpathTest = addJacocoToClasspath(mavenProject.getBasedir().getAbsolutePath() + "/target/test-classpath");
                // collect test classes
                StringBuilder entryParametersTest = new StringBuilder();
                for (String test : findTestFiles(mavenProject.getBuild().getTestOutputDirectory())) {
                    StringBuilder testSb = new StringBuilder(test);
                    entryParametersTest.append(testSb.append(" "));
                }
                // execute all the tests classes
                CmdExec cmdExecTestDebloat = new CmdExec();
                Set<String> classesLoadedTestDebloat = cmdExecTestDebloat.execProcess(
                        classpathTest + ":" + mavenProject.getBuild().getOutputDirectory() + ":" + mavenProject.getBuild().getTestOutputDirectory(),
                        "org.junit.runner.JUnitCore",
                        entryParametersTest.toString().split(" "));
                // print info about the number of classes loaded
                LOGGER.info("Number of classes loaded: " + classesLoadedTestDebloat.size());
                ClassesLoadedSingleton.INSTANCE.setClassesLoaded(classesLoadedTestDebloat);
                // list the classes loaded
                ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
                break;
            case ENTRY_POINT_DEBLOAT:
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
                // list the classes loaded
                ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
                break;
            case CONSERVATIVE_DEBLOAT:
                // TODO implement the conservative approach
                break;
        }

        // move the jacoco exec file to the target dir
        FileUtils.moveFile(new File(mavenProject.getBasedir(), "jacoco.exec"), new File(mavenProject.getBasedir(), "target/jacoco.exec"));

        // restore instrumented classes and generate the jacoco xml report
        mavenUtils.runMaven(Arrays.asList(
                "org.jacoco:jacoco-maven-plugin:0.8.4:restore-instrumented-classes",
                "org.jacoco:jacoco-maven-plugin:0.8.4:report"), null);

        // move the jacoco xml report
        FileUtils.moveFile(new File(mavenProject.getBasedir(), "target/site/jacoco/jacoco.xml"), report);

        // read the jacoco report
        JacocoReportReader reportReader = new JacocoReportReader();

        return reportReader.getUnusedClassesAndMethods(report);
    }

    private String addJacocoToClasspath(String file) throws IOException {
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

    /**
     * Recursively search for class files in a directory.
     *
     * @param testOutputDirectory
     * @return the name of tests files present in a given directory.
     */
    private List<String> findTestFiles(String testOutputDirectory) {

        File f = new File(testOutputDirectory);
        File[] list = f.listFiles();
        assert list != null;
        for (File testFile : list) {
            if (testFile.isDirectory()) {
                findTestFiles(testFile.getAbsolutePath());
            } else if (testFile.getName().endsWith(".class")) {
                String testName = testFile.getAbsolutePath();
                // Get the binary name of the test file
                System.out.println("added tests: " + testName);

                tests.add(testName.replaceAll("/", ".")
                        .substring(mavenProject.getBuild().getTestOutputDirectory().length() + 1, testName.length() - 6));
            }
        }
        return tests;
    }
}
