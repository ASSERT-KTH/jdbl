package se.kth.castor.jdbl.util;

public class JarWithDeps
{
    private static JarWithDeps single_instance = null;

    private String name;
    private String path;

    private JarWithDeps(String name, String path)
    {
        this.name = name;
        this.path = path;
    }

    public static JarWithDeps setInstance(String name, String path)
    {
        if (single_instance == null) {
            single_instance = new JarWithDeps(name, path);
        }

        return single_instance;
    }

    public static JarWithDeps getInstance()
    {
        if (single_instance == null) {
            System.out.println("Error, jar-with-dependencies not found.");
        }

        return single_instance;
    }

    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return path;
    }
}
