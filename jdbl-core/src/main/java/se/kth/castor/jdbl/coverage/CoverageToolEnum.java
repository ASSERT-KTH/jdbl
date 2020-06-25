package se.kth.castor.jdbl.coverage;

public enum CoverageToolEnum
{
    YAJTA("Yajta"),
    JACOCO("JaCoCo"),
    JVM_CLASS_LOADER("JVM");

    private String tool;

    CoverageToolEnum(String tool)
    {
        this.tool = tool;
    }

    public String getName()
    {
        return tool;
    }
}
