package se.kth.jdbl.instrumenter;

import java.util.HashSet;
import java.util.Set;

public class UsageLogger {

    private static Set<Class<?>> covered = new HashSet<>();

    public static Set<Class<?>> getCoveredClasses() {
        return covered;
    }

    public static void resetCoverage() {
        for (Class<?> c : covered) {
            try {
                c.getField(ClassCoverageCV.CLASS_COVERAGE_FIELD).setBoolean(null, false);
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
            }
        }
        covered = new HashSet<>();
    }

    public static void classHit(Class<?> c) {
        covered.add(c);
    }
}
