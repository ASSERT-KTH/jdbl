package se.kth.jdbl.loader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntryPointClassLoader extends ClassLoader {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private ClassLoader parent;
    private String classDir;

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public EntryPointClassLoader(String classDir, ClassLoader parent) {
        this.classDir = classDir;
        this.parent = parent;
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    @Override
    public Class<?> loadClass(String name) {
        try {
            System.out.println("[Parent] Loading class: " + name);
            return parent.loadClass(name);
        } catch (ClassNotFoundException ex) {
            // Load this class separately
            try {
                byte[] original = Files.readAllBytes(Paths.get(classDir + "/" + name.replace(".", "/") + ".class"));
//                System.out.println("[EntryPointClassLoader] Loading class: " + name);
                return defineClass(name.replace("/", "."), original, 0, original.length);
            } catch (Exception e) {
                System.err.println("[EntryPointClassLoader] Error: class: " + name + " path: " + classDir + "/" + name.replace(".", "/") + ".class, exception: " + e);
            }
        }
        System.err.println("[EntryPointClassLoader] Loading class: " + name + " failed.");
        return null;
    }

    @Override
    public URL getResource(String name) {
        System.out.println("[EntryPointClassLoader] getting resource: " + name);
        try {
            URL resource = new File(classDir + "/resources/" + name).toPath().toUri().toURL();
            System.out.println("Path to the resource: " + resource.getPath());
            return getParent().getResource(name);
//            return resource;
        } catch (MalformedURLException e) {
            System.err.println("[EntryPointClassLoader] Error getting resource: " + name);
            return getParent().getResource(name);
        }
    }
}








