package se.kth.castor.jdbl.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.app.DebloatTypeEnum;
import se.kth.castor.jdbl.app.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.app.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.app.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.app.util.JDblFileUtils;
import se.kth.castor.jdbl.app.util.JarUtils;
import se.kth.castor.jdbl.app.wrapper.JacocoWrapper;

/**
 * This Mojo instruments the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered classes are removed from the final jar file, the non covered
 * methods is replaced by an <code>UnsupportedOperationException</code>.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractDebloatMojo
{
   @Override
   public void doExecute()
   {
      printCustomStringToConsole("S T A R T I N G    T E S T    B A S E D    D E B L O A T");
      Instant start = Instant.now();
      cleanReportFile();
      String outputDirectory = getProject().getBuild().getOutputDirectory();

      // run JaCoCo usage analysis
      Map<String, Set<String>> jaCoCoUsageAnalysis = this.getJaCoCoUsageAnalysis();
      Set<String> usedClasses = null;
      try {
         this.printClassesLoaded();
         usedClasses = TestBasedDebloatMojo.getUsedClasses(jaCoCoUsageAnalysis);
      } catch (RuntimeException e) {
         this.getLog().error("Error computing JaCoCo usage analysis.");
      }

      // write to a file with the status of classes (Used or Bloated) in each dependency
      try {
         writeClassStatusPerDependency(usedClasses);
      } catch (RuntimeException e) {
         this.getLog().error("Error writing the status of classes per dependency.");
      }

      // remove unused classes
      this.getLog().info("Starting removing unused classes...");
      this.removeUnusedClasses(outputDirectory, usedClasses);

      // remove unused methods
      this.getLog().info("Starting removing unused methods...");
      this.removeUnusedMethods(outputDirectory, jaCoCoUsageAnalysis);

      // ----------------------------------------------------
      rerunTests();
      // ----------------------------------------------------

      // write log file with the plugin's execution time
      writeTimeElapsedReportFile(start);
      printCustomStringToConsole("T E S T S    B A S E D    D E B L O A T    F I N I S H E D");
   }

   private void rerunTests()
   {
      Runtime rt = Runtime.getRuntime();
      Process p = null;
      try {
         p = rt.exec("mvn test");
      } catch (IOException e) {
         this.getLog().error("Error executing the test suite.");
      }
      final Process finalP = p;
      new Thread(() -> {
         BufferedReader input = new BufferedReader(new InputStreamReader(finalP.getInputStream()));
         String line;
         int totalTests = 0;
         int failedTests = 0;
         int errorTests = 0;
         int skippedTests = 0;
         try {
            while ((line = input.readLine()) != null) {
               if (Pattern.matches("^Tests run: \\d*, Failures: \\d*, Errors: \\d*, Skipped: \\d*$", line)) {
                  line = line.replaceAll("\\s+", "");
                  String[] split = line.split(",");
                  totalTests = Integer.parseInt(split[0].split(":")[1]);
                  failedTests = Integer.parseInt(split[1].split(":")[1]);
                  errorTests = Integer.parseInt(split[2].split(":")[1]);
                  skippedTests = Integer.parseInt(split[3].split(":")[1]);
               }
            }
            if (errorTests != 0) {
               this.getLog().info("Tests run: " + totalTests + ", Failures: " + failedTests +
                  ", Errors: " + errorTests + ", Skipped: " + skippedTests);
               this.getLog().error("Unable to produce a correct debloated version: tests failures!");
               System.exit(-1);
            }
         } catch (IOException e) {
            this.getLog().error("Error reading the test execution outputs.");
         }
      }).start();
   }

   private void writeClassStatusPerDependency(final Set<String> usedClasses)
   {
      final String reportClassStatusPerDependencyFileName = "debloat-dependencies-report.csv";
      this.getLog().info("Writing " + reportClassStatusPerDependencyFileName + " to " +
         new File(getProject().getBasedir().getAbsolutePath() + "/"));
      StringBuilder s = new StringBuilder();
      Set<JarUtils.DependencyFileMapper> dependencyFileMappers = JarUtils.getDependencyFileMappers();
      for (JarUtils.DependencyFileMapper fileMapper : dependencyFileMappers) {
         for (final String dependencyJarName : fileMapper.getDependencyClassMap().keySet()) {
            s.append(dependencyJarName).append("\n");
            for (final String classInTheDependency : fileMapper.getDependencyClassMap().get(dependencyJarName)) {
               if (usedClasses.contains(classInTheDependency)) {
                  s.append("\t" + "UsedClass, ").append(classInTheDependency).append("\n");
               } else {
                  s.append("\t" + "BloatedClass, ").append(classInTheDependency).append("\n");
               }
            }
         }
      }
      try {
         org.apache.commons.io.FileUtils.write(new File(getProject().getBasedir().getAbsolutePath() + "/" +
            reportClassStatusPerDependencyFileName), s);
      } catch (IOException e) {
         this.getLog().error("Error creating dependency bloat report file.");
      }
   }

   private void writeTimeElapsedReportFile(final Instant start)
   {
      this.getLog().info("Writing dependency-bloat-report.csv to " +
         new File(getProject().getBasedir().getAbsolutePath() + "/"));
      Instant finish = Instant.now();
      double timeElapsed = Duration.between(start, finish).toMillis();
      final String timeElapsedInSeconds = "Total debloat time: " + timeElapsed / 1000 + " s";
      this.getLog().info(timeElapsedInSeconds);
      try {
         final String reportExecutionTimeFileName = "debloat-execution-time.log";
         org.apache.commons.io.FileUtils.write(new File(getProject().getBasedir().getAbsolutePath() + "/" +
            reportExecutionTimeFileName), timeElapsedInSeconds);
      } catch (IOException e) {
         this.getLog().error("Error creating time elapsed report file.");
      }
   }

   private void cleanReportFile()
   {
      try {
         org.apache.commons.io.FileUtils.write(new File(getProject().getBasedir().getAbsolutePath() + "/" +
            getReportFileName()), "");
      } catch (IOException e) {
         this.getLog().error("Error cleaning report file.");
      }
   }

   private void printClassesLoaded()
   {
      final int nbOfClassesLoaded = ClassesLoadedSingleton.INSTANCE.getClassesLoaded().size();
      this.getLog().info("Loaded classes (" + nbOfClassesLoaded + ')');
      ClassesLoadedSingleton.INSTANCE.getClassesLoaded().stream().forEach(System.out::println);
      this.getLog().info(getLineSeparator());
   }

   private void removeUnusedMethods(final String outputDirectory, final Map<String, Set<String>> usageAnalysis)
   {
      AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(outputDirectory,
         usageAnalysis,
         new File(getProject().getBasedir().getAbsolutePath() + "/" + getReportFileName()));
      try {
         testBasedMethodDebloat.removeUnusedMethods();
      } catch (IOException e) {
         this.getLog().error(String.format("Error: %s", e));
      }
   }

   private void removeUnusedClasses(final String outputDirectory, final Set<String> usedClasses)
   {
      JDblFileUtils JDblFileUtils = new JDblFileUtils(outputDirectory,
         new HashSet<>(),
         usedClasses,
         new File(getProject().getBasedir().getAbsolutePath() + "/" + getReportFileName()));
      try {
         JDblFileUtils.deleteUnusedClasses(outputDirectory);
      } catch (IOException e) {
         this.getLog().error(String.format("Error deleting unused classes: %s", e));
      }
   }

   private static Set<String> getUsedClasses(final Map<String, Set<String>> usageAnalysis)
   {
      // get the union of the JaCoCo output and the JVM class loader results
      Set<String> usedClasses = new HashSet<>(ClassesLoadedSingleton.INSTANCE.getClassesLoaded());
      usageAnalysis
         .entrySet()
         .stream()
         .filter(e -> e.getValue() != null)
         .forEach(className -> usedClasses.add(className.getKey().replace('/', '.')));
      return usedClasses;
   }

   private Map<String, Set<String>> getJaCoCoUsageAnalysis()
   {
      JacocoWrapper jacocoWrapper = new JacocoWrapper(getProject(),
         new File(getProject().getBasedir().getAbsolutePath() + "/target/report.xml"),
         DebloatTypeEnum.TEST_DEBLOAT);
      Map<String, Set<String>> usageAnalysis = null;
      try {
         usageAnalysis = jacocoWrapper.analyzeUsages();
         this.printJaCoCoUsageAnalysisResults(usageAnalysis);
      } catch (IOException | ParserConfigurationException | SAXException e) {
         this.getLog().error(e);
      }
      return usageAnalysis;
   }

   private void printJaCoCoUsageAnalysisResults(final Map<String, Set<String>> usageAnalysis)
   {
      this.getLog().info("JaCoCo ANALYSIS RESULTS:");
      this.getLog().info(String.format("Total unused classes: %d",
         usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count()));
      this.getLog().info(String.format("Total unused methods: %d",
         usageAnalysis.values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));
      this.getLog().info(getLineSeparator());
   }
}
