package se.kth.castor.jdbl.load;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class AbstractClassLoader extends ClassLoader
{

    private String classLoaderName;
    private String classDir;

    public AbstractClassLoader(String classLoaderName, String classDir)
    {
        this.classLoaderName = classLoaderName;
        this.classDir = classDir;
    }

    public String getClassDir()
    {
        return classDir;
    }

    @Override
    public URL getResource(String name)
    {
        System.out.println("[" + classLoaderName + "] getting resource: " + name);
        try {
            URL resource = new File(classDir + "/resources/" + name).toPath().toUri().toURL();
            System.out.println("Path to the resource: " + resource.getPath());
            return getParent().getResource(name);
        } catch (MalformedURLException e) {
            System.err.println("[" + classLoaderName + "] Error getting resource: " + name);
            return getParent().getResource(name);
        }
    }
}
