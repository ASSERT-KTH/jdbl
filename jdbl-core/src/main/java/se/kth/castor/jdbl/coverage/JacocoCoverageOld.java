package se.kth.castor.jdbl.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.deptree.OptionalDependencyIgnorer;
import se.kth.castor.jdbl.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.util.CmdExec;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

@Deprecated
public class JacocoCoverageOld extends AbstractCoverage implements UsageAnalyzer
{
    private static final Logger LOGGER = LogManager.getLogger(JacocoCoverageOld.class.getName());

    private List<String> tests;
    private File report;

    public JacocoCoverageOld(MavenProject mavenProject, File mavenHome, File report, DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
        this.report = report;
        this.tests = new ArrayList<>();
        if (report.exists()) {
            FileUtils.deleteQuietly(report);
        }
    }

    public JacocoCoverageOld(MavenProject mavenProject, File report, DebloatTypeEnum debloatTypeEnum, String entryClass,
        String entryMethod, String entryParameters, File mavenHome)
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

        // // Apply bytecode transformations (warning: it causes issues such as JaCoCo not covering dependencies)
        // Iterator<File> itFiles = FileUtils.iterateFiles(new File(classesDir), new String[]{"class"}, true);
        // while (itFiles.hasNext()) {
        //     File file = itFiles.next();
        //     FileInputStream fileInputStream = null;
        //     try {
        //         fileInputStream = new FileInputStream(file);
        //         ConstantAdapter constantAdapter = new ConstantAdapter(fileInputStream);
        //         byte[] result = constantAdapter.addField();
        //         FileUtils.forceDelete(file);
        //         FileUtils.writeByteArrayToFile(new File(file.getAbsolutePath()), result);
        //         fileInputStream.close();
        //     } catch (IOException e) {
        //         LOGGER.error("Error applying bytecode transformation.");
        //     }
        // }

        // Instrument the code
        mavenUtils.runMaven(Collections.singletonList("org.jacoco:jacoco-maven-plugin:0.8.4:instrument"), null);

        switch (this.debloatTypeEnum) {
            case TEST_DEBLOAT:
                try {
                    JVMClassCoverage jvmClassCoverage = new JVMClassCoverage();
                    jvmClassCoverage.runTestsInVerboseMode();
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

}
