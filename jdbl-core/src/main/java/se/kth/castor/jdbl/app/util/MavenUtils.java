package se.kth.castor.jdbl.app.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class MavenUtils
{
   private static final int TEST_EXECUTION_TIMEOUT = 10 * 60; // 10 minutes in seconds

   private final File mavenHome;
   private final File workingDir;

   private static final Logger LOGGER = LogManager.getLogger(MavenUtils.class.getName());

   public MavenUtils(File mavenHome, File workingDir)
   {
      this.mavenHome = mavenHome;
      this.workingDir = workingDir;
   }

   /**
    * Execute the maven plugin dependency:copy-dependencies.
    * This plugin resolves direct and transitive dependencies.
    *
    * @param outputDirectory The directory to put the dependencies in.
    */
   public void copyDependencies(String outputDirectory)
   {
      Properties copyDependenciesProperties = new Properties();
      copyDependenciesProperties.setProperty("outputDirectory", outputDirectory);
      copyDependenciesProperties.setProperty("includeScope", "compile");
      copyDependenciesProperties.setProperty("prependGroupId", "true");
      runMaven(Collections.singletonList("dependency:copy-dependencies"), copyDependenciesProperties);
   }


   public void copyDependency(String artifact, String outputDirectory)
   {
      LOGGER.info("Copying dependency " + artifact + " to " + outputDirectory);
      Properties copyProperties = new Properties();
      copyProperties.setProperty("artifact", artifact);
      runMaven(Collections.singletonList("dependency:get"), copyProperties);

      copyProperties = new Properties();
      copyProperties.setProperty("artifact", artifact);
      copyProperties.setProperty("outputDirectory", outputDirectory);
      runMaven(Collections.singletonList("dependency:copy"), copyProperties);
   }

   public void dependencyTree(String outputDirectory)
   {
      LOGGER.info("Copying dependency tree to " + outputDirectory + "/dependency-tree.txt");
      Properties copyProperties = new Properties();
      copyProperties.setProperty("outputFile", outputDirectory);
      copyProperties.setProperty("outputType", "text");
      runMaven(Collections.singletonList("dependency:tree"), copyProperties);
   }

   /**
    * Copy the resources to the specified directory.
    * Resolve direct and transitive resources.
    *
    * @param outputDirectory Directory to put the resources in.
    */
   public void copyResources(String outputDirectory)
   {
      Properties copyResourcesProperties = new Properties();
      copyResourcesProperties.setProperty("outputDirectory", outputDirectory + "/resources");
      runMaven(Collections.singletonList("resources:resources"), copyResourcesProperties);
   }

   /**
    * General method used to run maven goals based on a specified set of properties.
    */
   public void runMaven(List<String> goals, Properties properties)
   {
      File pomFile = new File(workingDir, "pom.xml");
      InvocationRequest request = new DefaultInvocationRequest();
      request.setBatchMode(true);
      request.setPomFile(pomFile);
      if (properties != null) {
         request.setProperties(properties);
      }
      request.setGoals(goals);
      request.setTimeoutInSeconds(TEST_EXECUTION_TIMEOUT);
      Invoker invoker = new DefaultInvoker();
      invoker.setMavenHome(mavenHome);
      invoker.setWorkingDirectory(workingDir);
      try {
         InvocationResult result = invoker.execute(request);
         result.getExitCode();
      } catch (MavenInvocationException e) {
         LOGGER.error("Unable to run Maven: " + e);
      }
   }
}
