package se.kth.castor.jdbl.app.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import se.kth.castor.jdbl.app.DebloatTypeEnum;
import se.kth.castor.jdbl.app.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.app.util.CmdExec;
import se.kth.castor.jdbl.app.util.JarUtils;
import se.kth.castor.jdbl.app.util.MavenUtils;

public class JacocoWrapper
{
   private static final Logger LOGGER = LogManager.getLogger(JacocoWrapper.class.getName());
   private MavenProject mavenProject;
   private String entryClass;
   private String entryMethod;
   private String entryParameters;
   private List<String> tests;
   private File mavenHome;
   private File report;
   private DebloatTypeEnum debloatTypeEnum;
   private boolean isJunit5 = false;

   public JacocoWrapper(MavenProject mavenProject,
      File report,
      DebloatTypeEnum debloatTypeEnum)
   {
      this.mavenProject = mavenProject;
      this.report = report;
      this.debloatTypeEnum = debloatTypeEnum;
      this.tests = new ArrayList<>();
      if (report.exists()) {
         FileUtils.deleteQuietly(report);
      }
   }

   public JacocoWrapper(MavenProject mavenProject,
      File report,
      DebloatTypeEnum debloatTypeEnum,
      String entryClass,
      String entryMethod,
      String entryParameters,
      File mavenHome)
   {
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

   public Map<String, Set<String>> analyzeUsages() throws IOException, ParserConfigurationException, SAXException
   {
      MavenUtils mavenUtils = new MavenUtils(this.mavenHome, this.mavenProject.getBasedir());
      Properties propertyTestClasspath = new Properties();
      propertyTestClasspath.setProperty("mdep.outputFile", this.mavenProject.getBasedir().getAbsolutePath() +
         "/target/test-classpath");
      propertyTestClasspath.setProperty("scope", "test");

      // write all the test classpath to a local file
      mavenUtils.runMaven(Collections.singletonList("dependency:build-classpath"), propertyTestClasspath);

      Properties propertyCopyDependencies = new Properties();
      propertyCopyDependencies.setProperty("outputDirectory", this.mavenProject.getBasedir().getAbsolutePath() +
         "/target/classes");
      propertyCopyDependencies.setProperty("includeScope", "compile");
      mavenUtils.runMaven(Collections.singletonList("dependency:copy-dependencies"), propertyCopyDependencies);

      // do not process the optional dependencies
      OptionalDependencyIgnorer optionalDependencyIgnorer = new OptionalDependencyIgnorer(getMavenProject());
      optionalDependencyIgnorer.removeOptionalDependencies(mavenUtils);

      JarUtils.decompressJars(this.mavenProject.getBasedir().getAbsolutePath() + "/target/classes");

      // instrument the code
      mavenUtils.runMaven(Collections.singletonList("org.jacoco:jacoco-maven-plugin:0.8.4:instrument"), null);

      switch (this.debloatTypeEnum) {
         case TEST_DEBLOAT:
            this.testBasedDebloat();
            break;
         case ENTRY_POINT_DEBLOAT:
            this.entryPointDebloat();
            break;
         case CONSERVATIVE_DEBLOAT:
            // TODO implement the conservative approach
            break;
      }

      // restore instrumented classes and generate the jacoco xml report
      mavenUtils.runMaven(Arrays.asList(
         "org.jacoco:jacoco-maven-plugin:0.8.4:restore-instrumented-classes",
         "org.jacoco:jacoco-maven-plugin:0.8.4:report"), null);

      // copy the jacoco xml report
      FileUtils.copyFile(new File(this.mavenProject.getBasedir(), "target/site/jacoco/jacoco.xml"), this.report);

      // read the jacoco report
      JacocoReportReader reportReader = new JacocoReportReader();

      return reportReader.getUnusedClassesAndMethods(this.report);
   }

   public MavenProject getMavenProject()
   {
      return mavenProject;
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
      // list the classes loaded
      ClassesLoadedSingleton.INSTANCE.printClassesLoaded();
   }

   private void testBasedDebloat() throws IOException
   {
      Set<String> classesLoadedTestDebloat = new HashSet<>();
      Runtime rt = Runtime.getRuntime();
      FileUtils.deleteDirectory(new File("target/classes"));
      Process p = rt.exec("mvn test -X -Djacoco.skip -Denforcer.skip=true -Dcheckstyle.skip=true -Dcobertura.skip=true -DskipITs=true -Drat.skip=true -Dlicense.skip=true -Dfindbugs.skip=true -DargLine=\"-verbose:class\"");
      new Thread(() -> {
         BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
         String line;
         try {
            while ((line = input.readLine()) != null) {
               if (line.contains("class,load") && line.endsWith("target/classes/")) {
                  classesLoadedTestDebloat.add(line.split(" ")[1]);
               }
            }
         } catch (IOException e) {
            // should not happen
            LOGGER.error(e);
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

   private String addJacocoToClasspath(String file) throws IOException
   {
      StringBuilder rawFile;
      try (BufferedReader buffer = new BufferedReader(new FileReader(file))) {
         rawFile = new StringBuilder(this.mavenProject.getBasedir().getAbsolutePath() + "/target/classes/:");
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
