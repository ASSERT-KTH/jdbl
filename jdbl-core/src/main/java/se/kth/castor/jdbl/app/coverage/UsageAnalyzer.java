package se.kth.castor.jdbl.app.coverage;

import java.util.Map;
import java.util.Set;

public interface UsageAnalyzer
{
    Map<String, Set<String>> analyzeUsages();
}
