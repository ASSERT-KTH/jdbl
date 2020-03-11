package se.kth.castor.jdbl.debloat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMethodDebloat
{

   protected String outputDirectory;
   protected Map<String, Set<String>> usageAnalysis;
   protected File reportFile;

   public AbstractMethodDebloat(String outputDirectory, Map<String, Set<String>> usageAnalysis, File reportFile)
   {
      this.outputDirectory = outputDirectory;
      this.usageAnalysis = usageAnalysis;
      this.reportFile = reportFile;
   }

   /**
    * Iterate through the used classes and replace the body of bloated methods with
    * an <code>UnsupportedOperationException</code>.
    */
   public void removeUnusedMethods() throws IOException
   {
      for (Map.Entry<String, Set<String>> entry : this.usageAnalysis.entrySet()) {
         if (entry.getValue() != null) {
            this.removeMethod(entry.getKey().replace(".", "/"), entry.getValue());
         }
      }
   }

   public abstract void removeMethod(String clazz, Set<String> usedMethods) throws IOException;
}
