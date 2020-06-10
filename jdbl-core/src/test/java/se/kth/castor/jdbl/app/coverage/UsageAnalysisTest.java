package se.kth.castor.jdbl.app.coverage;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsageAnalysisTest
{
    UsageAnalysis usageAnalysis;

    @Before
    public void setUp() throws Exception
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
}
