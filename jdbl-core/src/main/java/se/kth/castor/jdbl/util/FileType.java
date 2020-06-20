package se.kth.castor.jdbl.util;

/**
 * The debloat strategies supported by this plugin.
 */
public enum FileType
{
    ANNOTATION,
    ENUM,
    INTERFACE,
    CONSTANT,
    CLASS,
    EXCEPTION,
    UNKNOWN;

    @Override
    public String toString()
    {
        return this.name();
    }
}
