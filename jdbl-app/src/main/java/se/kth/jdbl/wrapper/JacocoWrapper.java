package se.kth.jdbl.wrapper;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private String entryClass;
    private String entryMethod;
    private String entryParameters;

    private File mavenHome;
    private File report;

    private DebloatTypeEnum debloatTypeEnum;

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public JacocoWrapper(File report, DebloatTypeEnum debloatTypeEnum, String entryClass, String entryMethod, String entryParameters, File mavenHome) {
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
                mavenUtils.runMaven(Collections.singletonList("test"), null);
                break;
            case ENTRY_POINT_DEBLOAT:
                CmdExec cmdExec = new CmdExec();
                LOGGER.info("Output directory: " + mavenProject.getBuild().getOutputDirectory());
                LOGGER.info("entryClass: " + entryClass);
                LOGGER.info("entryMethod: " + entryMethod);
                LOGGER.info("entryParameters: " + entryParameters);

                System.out.println("starting debloat execution");

                // add jacoco to the classpath
                String classpath = addJacocoToClasspath(mavenProject.getBasedir().getAbsolutePath() + "/target/test-classpath");

                // execute the application from entry point
                Set<String> classesLoaded = cmdExec.execProcess(classpath, entryClass, entryParameters.split(" "));

                System.out.println("Number of classes loaded: " + classesLoaded.size());

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

//    private URLClassLoader createClassLoader(File in) throws IOException {
//        BufferedReader buffer = new BufferedReader(new FileReader(in));
//        StringBuilder rawFile = new StringBuilder(mavenProject.getBasedir().getAbsolutePath() + "/target/classes/:");
//        String line;
//        while ((line = buffer.readLine()) != null) {
//            rawFile.append(line);
//        }
//        URL[] urls = Arrays.stream(rawFile.toString().split(":"))
//                .map(str -> {
//                    try {
//                        return new URL("file://" + str);
//                    } catch (MalformedURLException e) {
//                        LOGGER.error("failed to add to classpath: " + str);
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .toArray(URL[]::new);
//
//        for (URL url : urls) {
//            LOGGER.info("url: " + url.getPath());
//        }
//        return new URLClassLoader(urls);
//    }
}
