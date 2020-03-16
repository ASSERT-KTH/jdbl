package se.kth.castor.jdbl.app.debloat;

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
    * Replace the body of bloated methods with <code>UnsupportedOperationException</code> in the class.
    *
    * @param clazz       The fully qualified name of the used  class.
    * @param usedMethods The name of the used methods in this class.
    */
   public abstract void removeMethod(String clazz, Set<String> usedMethods) throws IOException;

   /**
    * Iterate through the used classes to replace the body of bloated methods with
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
}
