package se.kth.jdbl.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import se.kth.jdbl.agent.MethodProfiler;

import java.lang.instrument.Instrumentation;

public class Premain {

    /**
     * Duplicate of premain(), needed for ea-agent-loader in tests
     *
     * @param agentArguments
     * @param instrumentation
     * @throws Exception
     */
    public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception {
        premain(agentArguments, instrumentation);
    }

    public static void premain(String agentArgs,
                               Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            //For our purposes, skip java* and sun* internal methods
            if (className.startsWith("java") || className.startsWith("sun"))
                return null;
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            try {
                ClassVisitor cv = new MethodProfiler(cw);
                cr.accept(cv, 0);
                return cw.toByteArray();
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        });
    }

}