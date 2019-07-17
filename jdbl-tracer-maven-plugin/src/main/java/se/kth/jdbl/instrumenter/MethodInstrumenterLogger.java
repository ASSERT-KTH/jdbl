package se.kth.jdbl.instrumenter;

import java.lang.reflect.Method;
import java.util.HashSet;

public class MethodInstrumenterLogger {

    /**
     * The classes traced.
     */
    static HashSet<Class<?>> classesUsed = new HashSet<>();

    /**
     * The methods executed.
     */
    static HashSet<String> methodsUsed;

    /**
     * Callback called when a classfile is covered the first time.
     * State is reset upon calling dump().
     *
     * @param c
     */
    public static void classHit(Class<?> c) {
        classesUsed.add(c);
    }

    /**
     * Callback called when a method is covered the first time.
     * The callback occurs only when dump() is called - it is not called in real time.
     * State is reset upon calling dump.
     *
     * @param method
     */
    public static void methodHit(String method) {
        methodsUsed.add(method);
    }

    /**
     * Saves the list of methods covered since the last invocation of dump().
     *
     */
    public static void dump() {
        HashSet<Class<?>> classes = classesUsed;
        classesUsed = new HashSet<>();
        methodsUsed = new HashSet<>();
        for (Class<?> c : classes) {
            try {
                Method m = c.getDeclaredMethod("__dumpMethodsHit");
                m.setAccessible(true);
                m.invoke(null);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        UsageRecorder.setUsedMethods(methodsUsed);
    }
}
