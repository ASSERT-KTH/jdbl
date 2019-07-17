package se.kth.jdbl.loader;

import se.kth.jdbl.instrumenter.MethodInstrumenterLogger;
import se.kth.jdbl.instrumenter.UsageRecorder;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntryPointClassLoader extends ClassLoader {
    private String classPath;
    private ClassFileTransformer transformer;

    public EntryPointClassLoader(ClassFileTransformer transformer, String classPath) {
        this.transformer = transformer;
        this.classPath = classPath;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class cl = super.loadClass(name);
//            System.out.println("[Parent] Loading class: " + name);
            return cl;
        } catch (ClassNotFoundException ex) {
            // Load this class separately
            if (name.startsWith("se.kth.jdbl.instrumenter.MethodInstrumenterLogger")) {
                return MethodInstrumenterLogger.class.getClassLoader().loadClass(name);
//            System.out.println("[Child] Loading class: " + name);
//            return getParent().loadClass(name);
            }
            try {
                byte[] original = Files.readAllBytes(Paths.get(classPath + "/" + name.replace(".", "/") + ".class"));
                byte[] byteBuffer = transformer.transform(this, name, null, null, original);
//                System.out.println("[EntryPointClassLoader] Loading class: " + name);
                UsageRecorder.addUsedClass(name);
                return defineClass(name.replace("/", "."), byteBuffer, 0, byteBuffer.length);
            } catch (Exception e) {
                System.out.println("[EntryPointClassLoader] Error: class: " + name + " path: " + classPath + "/" + name.replace(".", "/") + ".class, exception: " + e);
            }
        }
        System.err.println("[EntryPointClassLoader] Loading class: " + name + " failed.");
        return null;
    }

    @Override
    public URL getResource(String name) {
        System.out.println("[EntryPointClassLoader] get resource: " + name);
        try {
            URL resource = new File(classPath + "/" + name).toPath().toUri().toURL();
            System.out.println("Path to the resource: " + resource.getPath());
            return getParent().getResource(name);
//            return resource;
        } catch (MalformedURLException e) {
            System.err.println("[EntryPointClassLoader] Error get resource: " + name);
            return getParent().getResource(name);
        }
    }
}








