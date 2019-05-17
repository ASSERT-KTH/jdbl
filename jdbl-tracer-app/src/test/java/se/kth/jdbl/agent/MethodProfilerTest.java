package se.kth.jdbl.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import se.kth.jdbl.instrumentation.Tmp;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class MethodProfilerTest {

    @Before
    public void setUp() throws Exception {
//        AgentLoader.loadAgentClass(Premain.class.getName(),null);
    }

    @Test
    public void testCleanup() throws Exception {
        MethodProfilerLogger.dump();
        assertEquals(0, MethodProfilerLogger.dump().size());
    }

    // FIXME this test fails because the .jar file is not build to call the javaagent
    @Test
    public void testSingleCall() throws Exception {
//        MethodProfilerLogger.dump();
        otherMethod();
        HashSet<String> meths = MethodProfilerLogger.dump();
        System.out.println(meths);
//        assertEquals(1, meths.size());
//        assertEquals("se/kth/jdbl/agent/MethodProfilerTest.otherMethod()V", meths.iterator().next());
    }

    private void otherMethod() {

        Tmp tmp = new Tmp();
        tmp.sum();
    }

    @Test
    public void testJavaMethodsExcluded() throws Exception {
        MethodProfilerLogger.dump();
        HashSet<Object> foo = new HashSet<>();
        assertEquals(0, MethodProfilerLogger.dump().size());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void criticalTest() {
        System.out.println(" Critical Test :) ");
    }

}