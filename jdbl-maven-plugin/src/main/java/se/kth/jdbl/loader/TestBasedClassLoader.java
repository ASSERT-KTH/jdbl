package se.kth.jdbl.loader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

public class TestBasedClassLoader extends ClassLoader {

    private ClassLoader parent;
    private String testDir;
    private String classDir;

    public TestBasedClassLoader(String testDir, String classDir, ClassLoader parent) {
        this.testDir = testDir;
        this.classDir = classDir;
        this.parent = parent;
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {

        if (name.startsWith("org.junit")) {
            return parent.loadClass(name);
        }
        try {
            String qName = name.replace(".", "/") + ".class";

//            System.out.println("loading class: " + qName);

            File byteCode = null;

            File potential = new File(testDir + "/" + qName);
            if (!potential.exists()) {
                potential = new File(classDir + "/" + qName);
            }
            if (potential.exists()) {
                byteCode = potential;
            }
            if (byteCode != null) {
                byte[] original = Files.readAllBytes(byteCode.toPath());
                Class c = defineClass(name.replace("/", "."), original, 0, original.length);
                return c;
            }
        } catch (Exception e) {
            System.err.println("[TestBasedClassLoader] Error: class: " + name + ", ex: " + e);
        }
        return getParent().loadClass(name);
    }


    @Override
    public URL getResource(String name) {
        System.out.println("[TestBasedClassLoader] getting resource: " + name);
        try {
            URL resource = new File(classDir + "/resources/" + name).toPath().toUri().toURL();
            System.out.println("Path to the resource: " + resource.getPath());
            return getParent().getResource(name);
//            return resource;
        } catch (MalformedURLException e) {
            System.err.println("[TestBasedClassLoader] Error getting resource: " + name);
            return getParent().getResource(name);
        }
    }
}
