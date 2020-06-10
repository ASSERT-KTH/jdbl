package se.kth.castor.jdbl.app.coverage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UsageAnalysis
{
    Map<String, Set<String>> analysis;

    public UsageAnalysis()
    {
        this.analysis = new HashMap<>();
    }

    public void setAnalysis(final Map<String, Set<String>> analysis)
    {
        this.analysis = analysis;
    }

    public Set<String> methods(String clazz)
    {
        return analysis.get(clazz);
    }

    public void addEntry(String clazz, Set<String> methods)
    {
        analysis.put(clazz, methods);
    }

    public Map<String, Set<String>> getAnalysis()
    {
        return analysis;
    }

    public boolean containsClazz(String clazz)
    {
        return analysis.containsKey(clazz);
    }

    public void removeUncoveredClasses()
    {
        analysis.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public Set<String> classes()
    {
        return analysis.keySet();
    }

    /**
     * Computes the union between the current usage analysis and another usage analysis.
     */
    public UsageAnalysis mergeWith(UsageAnalysis usageAnalysis)
    {
        UsageAnalysis mergedUsageAnalysis = new UsageAnalysis();
        mergeClasses(usageAnalysis);
        mergeMethods(usageAnalysis, mergedUsageAnalysis);
        return mergedUsageAnalysis;
    }

    private void mergeMethods(UsageAnalysis usageAnalysis, UsageAnalysis mergedUsageAnalysis)
    {
        for (String clazz : classes()) {
            Set<String> methods = methods(clazz);
            if (usageAnalysis.containsClazz(clazz)) {
                methods.addAll(usageAnalysis.methods(clazz));
            }
            mergedUsageAnalysis.addEntry(clazz, methods);
        }
    }

    private void mergeClasses(UsageAnalysis usageAnalysis)
    {
        for (String clazz : usageAnalysis.classes()) {
            if (!containsClazz(clazz)) {
                addEntry(clazz, usageAnalysis.methods(clazz));
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        analysis.forEach((key, value) -> sb.append(key + " : " + value + "\n"));
        return sb.toString();
    }
}
