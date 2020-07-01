package se.kth.castor.jdbl.coverage;

public enum UsageStatusEnum
{
    USED_CLASS("UsedClass"),
    PRESERVED_CLASS("PreservedClass"),
    BLOATED_CLASS("BloatedClass"),
    USED_METHOD("UsedMethod"),
    BLOATED_METHOD("BloatedMethod");

    private String status;

    UsageStatusEnum(String status)
    {
        this.status = status;
    }

    public String getName()
    {
        return status;
    }
}
