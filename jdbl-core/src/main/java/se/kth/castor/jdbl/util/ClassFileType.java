package se.kth.castor.jdbl.util;

/**
 * The debloat strategies supported by this plugin.
 */
public enum ClassFileType
{
    ANNOTATION,
    ENUM,
    INTERFACE,
    CONSTANT,
    CLASS,
    EXCEPTION,
    SINGLETON,
    CLASS_ABSTRACT,
    UNKNOWN;

    @Override
    public String toString()
    {
        return this.name();
    }
}
