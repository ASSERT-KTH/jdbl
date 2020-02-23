package se.kth.castor.jdbl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.util.ClassesLoadedSingleton;
import se.kth.castor.jdbl.util.FileUtils;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;
import se.kth.castor.jdbl.wrapper.DebloatTypeEnum;
import se.kth.castor.jdbl.wrapper.JacocoWrapper;

/**
 * This Mojo instruments the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered elements are removed from the final jar file.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractMojo {

   /**
    * The maven home file, assuming either an environment variable M2_HOME, or that mvn command exists in PATH.
    */
   private static final File mavenHome = new File(System.getenv().get("M2_HOME"));

   @Parameter(defaultValue = "${project}", readonly = true)
   private MavenProject project;

   @Override
   public void execute() {

      String outputDirectory = this.project.getBuild().getOutputDirectory();
      File baseDir = this.project.getBasedir();

      this.getLog().info("------------------------------------------------------------------------");
      this.getLog().info("S T A R T I N G    T E S T    B A S E D    D E B L O A T");
      this.getLog().info("------------------------------------------------------------------------");

      MavenUtils mavenUtils = new MavenUtils(TestBasedDebloatMojo.mavenHome, baseDir);

      // copy the dependencies
      mavenUtils.copyDependencies(outputDirectory);

      // copy the resources
      mavenUtils.copyResources(outputDirectory);

      // decompress the copied dependencies
      JarUtils.decompressJars(outputDirectory);

      // run JaCoCo usage analysis
      Map<String, Set<String>> jaCoCoUsageAnalysis = this.getJaCoCoUsageAnalysis();
      Set<String> usedClasses = null;
      try {
         this.printClassesLoaded();
         usedClasses = TestBasedDebloatMojo.getUsedClasses(jaCoCoUsageAnalysis);

      } catch (RuntimeException e) {
         this.getLog().error("Error computing JaCoCo usage analysis");
      }

      // remove unused classes
      this.getLog().info("starting removing unused classes...");
      this.removeUnusedClasses(outputDirectory, usedClasses);

      // remove unused methods
      this.getLog().info("starting removing unused methods...");
      this.removeUnusedMethods(outputDirectory, jaCoCoUsageAnalysis);

      this.getLog().info("------------------------------------------------------------------------");
      this.getLog().info("T E S T S    B A S E D    D E B L O A T    F I N I S H E D");
      this.getLog().info("------------------------------------------------------------------------");
   }

   private void printClassesLoaded() {
      this.getLog().info("Loaded classes (" + ClassesLoadedSingleton.INSTANCE.getClassesLoaded().size() + ')');
      ClassesLoadedSingleton.INSTANCE.getClassesLoaded().stream().forEach(System.out::println);
      this.getLog().info("-------------------------------------------------------");
   }

   private void removeUnusedMethods(final String outputDirectory, final Map<String, Set<String>> usageAnalysis) {
      AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(outputDirectory, usageAnalysis);
      try {
         testBasedMethodDebloat.removeUnusedMethods();
      } catch (IOException e) {
         this.getLog().error(String.format("Error: %s", e));
      }
   }

   private void removeUnusedClasses(final String outputDirectory, final Set<String> usedClasses) {
      FileUtils fileUtils = new FileUtils(outputDirectory, new HashSet<>(), usedClasses);
      try {
         fileUtils.deleteUnusedClasses(outputDirectory);
      } catch (IOException e) {
         this.getLog().error(String.format("Error deleting unused classes: %s", e));
      }
   }

   private static Set<String> getUsedClasses(final Map<String, Set<String>> usageAnalysis) {
      // get the union of the JaCoCo output and the JVM class loader results
      Set<String> usedClasses = new HashSet<>();
      usedClasses.addAll(ClassesLoadedSingleton.INSTANCE.getClassesLoaded());
      usageAnalysis
         .entrySet()
         .stream()
         .filter(e -> e.getValue() != null)
         .forEach(className -> usedClasses.add(className.getKey().replace('/', '.')));
      return usedClasses;
   }

   private Map<String, Set<String>> getJaCoCoUsageAnalysis() {
      JacocoWrapper jacocoWrapper = new JacocoWrapper(this.project,
         new File(this.project.getBasedir().getAbsolutePath() + "/target/report.xml"),
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

   private void printJaCoCoUsageAnalysisResults(final Map<String, Set<String>> usageAnalysis) {
      this.getLog().info("JaCoCo ANALYSIS RESULTS:");
      this.getLog().info(String.format("Total unused classes: %d",
         usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count()));
      this.getLog().info(String.format("Total unused methods: %d",
         usageAnalysis.values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));
      this.getLog().info("-------------------------------------------------------");
   }
}
