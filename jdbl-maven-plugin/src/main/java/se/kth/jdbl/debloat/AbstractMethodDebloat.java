package se.kth.jdbl.debloat;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMethodDebloat {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    protected String outputDirectory;
    protected Map<String, Set<String>> usageAnalysis;

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public AbstractMethodDebloat(String outputDirectory, Map<String, Set<String>> usageAnalysis) {
        this.outputDirectory = outputDirectory;
        this.usageAnalysis = usageAnalysis;
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    /**
     * Iterate through the used classes and replace the body of methods with an exception.
     */
    public void removeUnusedMethods() throws IOException {
        for (Map.Entry<String, Set<String>> entry : usageAnalysis.entrySet()) {
            if (entry.getValue() != null) {
                removeMethod(entry.getKey().replace(".", "/"), entry.getValue());
            }
        }
    }

    public abstract void removeMethod(String clazz, Set<String> usedMethods) throws IOException;
}
