package se.kth.jdbl.agent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

public class MethodProfilerLogger {

    static HashSet<Class<?>> classesHit = new HashSet<>();
    static HashSet<String> methodsHit;

    /**
     * Callback called when a classfile is covered the first time.
     * State is reset upon calling dump().
     *
     * @param c
     */
    public static void classHit(Class<?> c) {
        classesHit.add(c);
    }

    /**
     * Callback called when a method is covered the first time.
     * The callback occurs only when dump() is called - it is not called in real time.
     * State is reset upon calling dump.
     *
     * @param method
     */
    public static void methodHit(String method) {
        methodsHit.add(method);
    }

    /**
     * Return the list of methods covered since the last invocation of dump()
     *
     * @return methods names
     */
    public static HashSet<String> dump() {
        HashSet<Class<?>> classes = classesHit;
        classesHit = new HashSet<>();
        methodsHit = new HashSet<>();
        for (Class<?> c : classes) {
            try {
                Method m = c.getDeclaredMethod("__dumpMethodsHit");
                m.setAccessible(true);
                m.invoke(null);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | NoSuchMethodException e) {
            }
        }
        return methodsHit;
    }
}
