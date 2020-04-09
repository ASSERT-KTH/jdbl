package se.kth.castor.jdbl.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractDebloatMojo extends AbstractMojo
{
   /**
    * The maven home file, assuming either an environment variable M2_HOME, or that mvn command exists in PATH.
    */
   private static final File mavenHome = new File(System.getenv().get("M2_HOME"));
   private static final String LINE_SEPARATOR = "------------------------------------------------------------------------";

   /**
    * The name of the file to be written in the root of the project with a report about the debloat results.
    */
   private String reportFileName = "debloat-report.csv";

   @Parameter(defaultValue = "${project}", readonly = true)
   private MavenProject project;

   /**
    * Skip plugin execution completely.
    */
   @Parameter(property = "skipJDBL", defaultValue = "false")
   private boolean skipKDBL;

   @Override
   public final void execute()
      throws MojoExecutionException, MojoFailureException
   {
      if (isSkipKDBL()) {
         getLog().info("Skipping plugin execution...");
         return;
      }
      this.doExecute();
   }

   protected abstract void doExecute()
      throws MojoExecutionException, MojoFailureException;

   public boolean isSkipKDBL()
   {
      return this.skipKDBL;
   }

   public MavenProject getProject()
   {
      return project;
   }

   public static File getMavenHome()
   {
      return mavenHome;
   }

   public void printCustomStringToConsole(final String s)
   {
      this.getLog().info(LINE_SEPARATOR);
      this.getLog().info(s);
      this.getLog().info(LINE_SEPARATOR);
   }

   public static String getLineSeparator()
   {
      return LINE_SEPARATOR;
   }

   public String getReportFileName()
   {
      return reportFileName;
   }
}
