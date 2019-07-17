package se.kth.jdbl.loader;

import se.kth.jdbl.instrumenter.MethodInstrumenterLogger;

import java.io.File;
import java.nio.file.Files;

public class TestBasedClassLoader extends ClassLoader {

    private ClassLoader parent;
    private String testDir;
    private String classDir;

    public TestBasedClassLoader(String testDir, String classDir, ClassLoader parent) {
        this.parent = parent;
        this.testDir = testDir;
        this.classDir = classDir;
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("se.kth.jdbl.instrumenter.MethodInstrumenterLogger")) {
            return MethodInstrumenterLogger.class.getClassLoader().loadClass(name);
        }
        if (name.startsWith("org.junit")) return parent.loadClass(name);
        //System.out.println("[TestBasedClassLoader] load " + name);
        try {
            boolean logClass = false;
            String qName = name.replace(".", "/") + ".class";
            File byteCode = null;

            File potential = new File(testDir + "/" + qName);
            //System.out.println(" trying " + potential.getAbsolutePath());
            if (!potential.exists()) {
                potential = new File(classDir + "/" + qName);
                logClass = true;
            }
            if (potential.exists()) {
                byteCode = potential;
            }
            if (byteCode != null) {
                byte[] original = Files.readAllBytes(byteCode.toPath());
                Class c = defineClass(name.replace("/", "."), original, 0, original.length);
                if (logClass) {
                    MethodInstrumenterLogger.classHit(c);
                }
                return c;
            }
        } catch (Exception e) {
            System.err.println("[EntryPointClassLoader] Error: class: " + name + ", ex: " + e);
        }
        return getParent().loadClass(name);
    }
}
