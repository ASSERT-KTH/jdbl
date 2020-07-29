package se.kth.castor.jdbl.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import se.kth.castor.jdbl.callgraph.JCallGraphModified;
import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.debloat.ConservativeMethodDebloat;
import se.kth.castor.jdbl.util.MyFileUtils;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

/**
 * This Maven mojo statically instruments the project and its dependencies in order to remove unused API members.
 */
@Mojo(name = "conservative-debloat", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class ConservativeDebloatMojo extends AbstractDebloatMojo
{
   @Override
   public void doExecute()
   {
      printCustomStringToConsole("S T A R T I N G    C O N S E R V A T I V E    D E B L O A T");

      String outputDirectory = getProject().getBuild().getOutputDirectory();
      File baseDir = getProject().getBasedir();

      MavenUtils mavenUtils = new MavenUtils(getMavenHome(), baseDir);

      // copy the dependencies
      mavenUtils.copyRuntimeDependencies(outputDirectory);

      // copy the resources
      mavenUtils.copyResources(outputDirectory);

      // decompress the copied dependencies
      JarUtils.decompressJars(outputDirectory);

      JCallGraphModified jCallGraphModified = new JCallGraphModified();

      // run de static usage analysis
      UsageAnalysis usageAnalysis = jCallGraphModified
         .runUsageAnalysis(getProject().getBuild().getOutputDirectory());

      Set<String> classesUsed = usageAnalysis.getAnalysis().keySet();

      this.getLog().info(String.format("#Total classes: %d",
         (long) usageAnalysis.getAnalysis().entrySet().size()));
      this.getLog().info(String.format("#Unused classes: %d",
         usageAnalysis.getAnalysis().entrySet().stream().filter(e -> e.getValue() == null).count()));
      this.getLog().info(String.format("#Unused methods: %d",
         usageAnalysis.getAnalysis().values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));

      // delete unused classes
      MyFileUtils MyFileUtils = new MyFileUtils(outputDirectory,
         new HashSet<>(),
         classesUsed,
         getProject().getBasedir().getAbsolutePath(), null);
      try {
         MyFileUtils.deleteUnusedClasses(outputDirectory, outputDirectory);
      } catch (IOException e) {
         this.getLog().error(String.format("Error deleting unused classes: %s", e));
      }

      // delete unused methods
      AbstractMethodDebloat conservativeMethodDebloat = new ConservativeMethodDebloat(outputDirectory,
         usageAnalysis,
         getProject().getBasedir().getAbsolutePath() + "/.jdbl/debloat-report.csv");
      try {
         conservativeMethodDebloat.removeUnusedMethods();
      } catch (IOException e) {
         this.getLog().error(String.format("Error: %s", e));
      }

      printCustomStringToConsole("C O N S E R V A T I V E    D E B L O A T    F I N I S H E D");
   }
}
