package se.kth.jdbl.instrumenter;

import java.util.HashSet;

public class UsageRecorder {

    private static HashSet<String> usedClasses = new HashSet<>();
    private static HashSet<String> usedMethods;

    /**
     * Private constructor.
     */
    private UsageRecorder() {
        throw new IllegalStateException("Utility class");
    }

    public static HashSet<String> getUsedClasses() {
        return usedClasses;
    }

    public static HashSet<String> getUsedMethods() {
        return usedMethods;
    }

    public static void setUsedMethods(HashSet<String> usedMethods) {
        UsageRecorder.usedMethods = usedMethods;
    }

    public static void setUsedClasses(HashSet<String> usedClasses) {
        UsageRecorder.usedClasses = usedClasses;
    }

    public static void addUsedClass(String className) {
        usedClasses.add(className);
    }

    public static void addUsedMethod(String methodName) {
        usedMethods.add(methodName);
    }

}
