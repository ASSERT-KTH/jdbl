package se.kth.castor.jdbl.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Agent {
    protected static final Set<String> calledMethods = new HashSet<>();

    public static void premain(String agentArgs, Instrumentation inst) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ArrayList<String> list = new ArrayList<>(calledMethods);
            list.sort(null);
            try (FileWriter heapFileWritter = new FileWriter(new File("agentCoverage.csv"), true)) {
                for (String calledMethod : list) {
                    heapFileWritter.append(calledMethod).append("\n").flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        AgentBuilder.Transformer.ForAdvice advice = new AgentBuilder.Transformer.ForAdvice()
                .include(inst.getClass().getClassLoader())
                .advice(ElementMatchers.any(), CallAdvice.class.getCanonicalName());

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("net.bytebuddy.")
                        .or(ElementMatchers.nameStartsWith("se.kth.castor.jdbl."))
                        .or(ElementMatchers.nameStartsWith("org.jacoco."))
                        .or(ElementMatchers.nameStartsWith("org.apache.maven."))
                        .or(ElementMatchers.nameStartsWith("org.objectweb.asm."))
                        .or(ElementMatchers.nameStartsWith("java."))
                        .or(ElementMatchers.nameStartsWith("jdk."))
                        .or(ElementMatchers.nameStartsWith("sun."))
                        .or(ElementMatchers.nameStartsWith("com.sun.")))
                .type(ElementMatchers.any()).transform(advice).installOn(inst);
    }

}
