package se.kth.castor.jdbl.coverage;

import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public enum JVMClassesCoveredSingleton
{
    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger(JVMClassesCoveredSingleton.class.getName());

    private Set<String> classesLoaded;

    protected void setClassesLoaded(Set<String> classesLoaded)
    {
        this.classesLoaded = classesLoaded;
    }

    public Set<String> getClassesLoaded()
    {
        return this.classesLoaded;
    }

    public void printClassesLoaded()
    {
        this.classesLoaded.forEach(s -> LOGGER.info("Loaded: " + s));
    }

}
