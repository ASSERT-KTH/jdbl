package se.kth.jdbl.util;

import java.util.Set;

public enum ClassesLoadedSingleton {

    INSTANCE;

    private Set<String > classesLoaded;

    public void setClassesLoaded(Set<String > count) {
        this.classesLoaded = count;
    }

    public Set<String> getClassesLoaded() {
        return classesLoaded;
    }

    public void printClassesLoaded() {
        this.classesLoaded.forEach(s -> System.out.println("Loaded: " + s));
    }

}