package se.kth.castor.jdbl.app.coverage;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsageAnalysisTest
{
    UsageAnalysis usageAnalysis;

    @Before
    public void setUp()
    {
        usageAnalysis = new UsageAnalysis();
        usageAnalysis.addEntry("calc/Calculator", new HashSet<>(Arrays.asList("<init>(II)V", "other()I", "sum()I")));
        usageAnalysis.addEntry("calc/CalculatorB", new HashSet<>(Arrays.asList("<init>()V")));
        usageAnalysis.addEntry("calc/StaticCl", new HashSet<>());
    }

    @Test
    public void removeUncoveredClasses()
    {
        usageAnalysis.removeUncoveredClasses();
        System.out.println(usageAnalysis.toString());
        assertEquals(2, usageAnalysis.analysis.size());
    }

    @Test
    public void mergeWith()
    {
        UsageAnalysis anotherUsageAnalysis = new UsageAnalysis();
        anotherUsageAnalysis.addEntry("calc/Calculator", new HashSet<>(Arrays.asList("<init>(II)V", "other()I", "sum()I", "multiply()I")));
        anotherUsageAnalysis.addEntry("calc/CalculatorA", new HashSet<>(Arrays.asList("<init>()V")));
        anotherUsageAnalysis.addEntry("calc/CalculatorB", new HashSet<>(Arrays.asList("<init>()V")));
        usageAnalysis.mergeWith(anotherUsageAnalysis);
        assertEquals(4, usageAnalysis.methods("calc/Calculator").size());
        assertEquals(4, usageAnalysis.classes().size());
    }
}
