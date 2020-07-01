package se.kth.castor.jdbl.load;

import java.nio.file.Files;
import java.nio.file.Paths;

public class EntryPointClassLoader extends AbstractClassLoader
{

    public EntryPointClassLoader(final String classLoaderName, final String classDir)
    {
        super(classLoaderName, classDir);
    }

    @Override
    public Class<?> loadClass(String name)
    {
        try {
            System.out.println("[Parent] Loading class: " + name);
            return getParent().loadClass(name);
        } catch (ClassNotFoundException ex) {
            // Load this class separately
            try {
                byte[] original = Files.readAllBytes(Paths.get(getClassDir() + "/" + name.replace(".", "/") + ".class"));
                System.out.println("[EntryPointClassLoader] Loading class: " + name);
                return defineClass(name.replace("/", "."), original, 0, original.length);
            } catch (Exception e) {
                System.err.println("[EntryPointClassLoader] Error: class: " + name + " path: " + getClassDir() + "/" + name.replace(".", "/") + ".class, exception: " + e);
            }
        }
        System.err.println("[EntryPointClassLoader] Loading class: " + name + " failed.");
        return null;
    }
}








