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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        analysis.forEach((key, value) -> sb.append(key + " : " + value + "\n"));
        return sb.toString();
    }
}
