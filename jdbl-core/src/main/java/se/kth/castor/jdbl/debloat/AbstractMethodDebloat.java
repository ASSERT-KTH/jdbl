package se.kth.castor.jdbl.debloat;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.util.MyFileWriter;

public abstract class AbstractMethodDebloat
{
    protected UsageAnalysis usageAnalysis;
    protected String outputDirectory;
    protected String projectBaseDir;
    protected int nbMethodsRemoved;
    protected MyFileWriter myFileWriter;

    public AbstractMethodDebloat(String outputDirectory, UsageAnalysis usageAnalysis, String projectBaseDir)
    {
        this.outputDirectory = outputDirectory;
        this.usageAnalysis = usageAnalysis;
        this.projectBaseDir = projectBaseDir;
        this.nbMethodsRemoved = 0;
        this.myFileWriter = new MyFileWriter(projectBaseDir);
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
        for (Map.Entry<String, Set<String>> entry : this.usageAnalysis.getAnalysis().entrySet()) {
            if (entry.getValue() != null) {
                this.removeMethod(entry.getKey().replace(".", "/"), entry.getValue());
            }
        }
    }

    public int nbMethodsRemoved()
    {
        return nbMethodsRemoved;
    }
}
