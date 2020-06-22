package se.kth.castor.jdbl.load;

import java.io.File;
import java.nio.file.Files;

public class TestBasedClassLoader extends ClassLoader
{
    private ClassLoader parent;
    private String testDir;
    private String classDir;

    public TestBasedClassLoader(String testDir, String classDir, ClassLoader parent)
    {
        this.testDir = testDir;
        this.classDir = classDir;
        this.parent = parent;
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException
    {
        if (name.startsWith("org.junit")) {
            return parent.loadClass(name);
        }
        try {
            String qName = name.replace(".", "/") + ".class";
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
                return defineClass(name.replace("/", "."), original, 0, original.length);
            }
        } catch (Exception e) {
            System.err.println("[TestBasedClassLoader] Error: class: " + name + ", ex: " + e);
        }
        return getParent().loadClass(name);
    }
}
