package se.kth.jdbl.callgraph;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

public class JCallGraphModifiedTest {

    private JCallGraphModified jCallGraphModified;
    private String classPath;

    @Before
    public void setUp() throws Exception {
        jCallGraphModified = new JCallGraphModified();
        classPath = "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jdbl-tracer/jdbl-maven-plugin/experiments/dummy-project/target/classes";
    }

    @Test
    public void processFile() {
        jCallGraphModified.getAllMethodsCallsFromFile(classPath)
                .forEach(System.out::println);
    }

    @Test
    public void getAllUsedClasses() {
        jCallGraphModified.runUsageAnalysis(classPath)
                .keySet()
                .forEach(System.out::println);
    }

    @Test
    public void getUsageAnalysis() {
        Map<String, Set<String>> usageAnalysis = jCallGraphModified.runUsageAnalysis(classPath);
        System.out.println(usageAnalysis);
    }
}
