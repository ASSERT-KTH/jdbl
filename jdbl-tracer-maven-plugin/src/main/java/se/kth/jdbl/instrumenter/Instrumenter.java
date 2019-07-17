package se.kth.jdbl.instrumenter;

import se.kth.jdbl.loader.EntryPointClassLoader;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Instrumenter {

    /**
     * The logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Instrumenter.class.getName());

    /**
     * The path of the file being instrumented.
     */
    private String curPath;

    /**
     * The number of classes instrumented.
     */
    private int nbClassesInstrumented = 0;

    /**
     * The binary name of the last instrumented class.
     */
    private String lastInstrumentedClass;

    /**
     * The output directory to place the instrumented classes.
     */
    private File rootOutputDir;

    /**
     * The custom class loader.
     */
    private EntryPointClassLoader loader;

    /**
     * The class file transformer.
     */
    private ClassFileTransformer transformer;

    /**
     * Set the custom class loader and class file transformer.
     *
     * @param customClassFileTransformer
     * @param customClassPath
     */
    public void setClassLoader(ClassFileTransformer customClassFileTransformer, String customClassPath) {
        loader = new EntryPointClassLoader(customClassFileTransformer, customClassPath);
    }

    /**
     * Set the custom class loader and class file transformer.
     *
     * @param customClassFileTransformer
     */
    public void setTransformer(ClassFileTransformer customClassFileTransformer) {
        transformer = customClassFileTransformer;
    }

    /**
     * Instrument the compiled classes located a given input directory.
     *
     * @param inputDirectory
     * @param outputDirectory
     */
    public void main(String inputDirectory, String outputDirectory) {
        instrumentDir(inputDirectory, outputDirectory);
    }

    /**
     * Instruments the classes, jar files or zip files in a directory.
     *
     * @param inputFolder
     * @param outputFolder
     */
    private void instrumentDir(String inputFolder, String outputFolder) {
        rootOutputDir = new File(outputFolder);
        if (!rootOutputDir.exists()) {
            rootOutputDir.mkdir();
        }
        File f = new File(inputFolder);
        if (!f.exists()) {
            LOGGER.severe("Unable to read path " + inputFolder);
            System.exit(-1);
        }
        if (f.isDirectory())
            processDirectory(f, rootOutputDir, true);
        else if (inputFolder.endsWith(".jar") || inputFolder.endsWith(".war"))
            processJar(f, rootOutputDir);
        else if (inputFolder.endsWith(".class"))
            try {
                processClass(f.getName(), new FileInputStream(f), rootOutputDir);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        else if (inputFolder.endsWith(".zip")) {
            processZip(f, rootOutputDir);
        } else {
            LOGGER.severe("Unknown type for path " + inputFolder);
            System.exit(-1);
        }
    }

    /**
     * This method instruments the classes by using a transformer an the custom class loader.
     *
     * @param path
     * @param is
     * @param renameInterfaces
     * @return
     */
    private byte[] instrumentClass(String path, InputStream is, boolean renameInterfaces) {
        try {
            nbClassesInstrumented++;
            curPath = path;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            // Call the ClassFileTransformer on the EntryPointClassLoader
            byte[] ret = transformer.transform(loader, path, null, null, buffer.toByteArray());
            curPath = null;
            return ret;
        } catch (Exception ex) {
            curPath = null;
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Process a given directory.
     *
     * @param f
     * @param parentOutputDir
     * @param isFirstLevel
     */
    private void processDirectory(File f, File parentOutputDir, boolean isFirstLevel) {
        File thisOutputDir;
        if (isFirstLevel) {
            thisOutputDir = parentOutputDir;
        } else {
            thisOutputDir = new File(parentOutputDir.getAbsolutePath() + "/" + f.getName());
            thisOutputDir.mkdir();
        }
        for (File fi : f.listFiles()) {
            if (fi.isDirectory())
                processDirectory(fi, thisOutputDir, false);
            else if (fi.getName().endsWith(".class"))
                try {
                    processClass(fi.getName(), new FileInputStream(fi), thisOutputDir);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            else if (fi.getName().endsWith(".jar") || fi.getName().endsWith(".war"))
                processJar(fi, thisOutputDir);
            else if (fi.getName().endsWith(".zip"))
                processZip(fi, thisOutputDir);
            else {
                File dest = new File(thisOutputDir.getPath() + "/" + fi.getName());
                FileChannel source = null;
                FileChannel destination = null;
                try {
                    source = new FileInputStream(fi).getChannel();
                    destination = new FileOutputStream(dest).getChannel();
                    destination.transferFrom(source, 0, source.size());
                } catch (Exception ex) {
                    LOGGER.severe("error copying file " + fi);
                    ex.printStackTrace();
                } finally {
                    if (source != null) {
                        try {
                            source.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (destination != null) {
                        try {
                            destination.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Instruments the a given jar file.
     *
     * @param f
     * @param outputDir
     */
    private void processJar(File f, File outputDir) {
        try {
            JarFile jar = new JarFile(f);
            JarOutputStream jos = null;
            jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + "/" + f.getName()));
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.getName().endsWith(".class")) {
                    {
                        try {
                            JarEntry outEntry = new JarEntry(e.getName());
                            jos.putNextEntry(outEntry);
                            byte[] clazz = instrumentClass(f.getAbsolutePath(), jar.getInputStream(e), true);
                            if (clazz == null) {
                                System.out.println("Failed to instrumenter " + e.getName() + " in " + f.getName());
                                InputStream is = jar.getInputStream(e);
                                byte[] buffer = new byte[1024];
                                while (true) {
                                    int count = is.read(buffer);
                                    if (count == -1)
                                        break;
                                    jos.write(buffer, 0, count);
                                }
                            } else {
                                jos.write(clazz);
                            }
                            jos.closeEntry();
                        } catch (ZipException ex) {
                            ex.printStackTrace();
                            continue;
                        }
                    }
                } else {
                    JarEntry outEntry = new JarEntry(e.getName());
                    if (e.isDirectory()) {
                        try {
                            jos.putNextEntry(outEntry);
                            jos.closeEntry();
                        } catch (ZipException exxx) {
                            System.out.println("Ignoring exception: " + exxx);
                        }
                    } else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
                        // don't copy this
                    } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
                        Scanner s = new Scanner(jar.getInputStream(e));
                        jos.putNextEntry(outEntry);
                        String curPair = "";
                        while (s.hasNextLine()) {
                            String line = s.nextLine();
                            if (line.equals("")) {
                                curPair += "\n";
                                if (!curPair.contains("SHA1-Digest:"))
                                    jos.write(curPair.getBytes());
                                curPair = "";
                            } else {
                                curPair += line + "\n";
                            }
                        }
                        s.close();
                        jos.closeEntry();
                    } else {
                        try {
                            jos.putNextEntry(outEntry);
                            InputStream is = jar.getInputStream(e);
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int count = is.read(buffer);
                                if (count == -1)
                                    break;
                                jos.write(buffer, 0, count);
                            }
                            jos.closeEntry();
                        } catch (ZipException ex) {
                            if (!ex.getMessage().contains("duplicate entry")) {
                                ex.printStackTrace();
                                System.out.println("Ignoring above warning from improper source zip...");
                            }
                        }
                    }
                }
            }
            if (jos != null) {
                jos.close();
            }
            jar.close();
        } catch (Exception e) {
            System.err.println("Unable to process jar: " + f.getAbsolutePath());
            e.printStackTrace();
            File dest = new File(outputDir.getPath() + "/" + f.getName());
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(f).getChannel();
                destination = new FileOutputStream(dest).getChannel();
                destination.transferFrom(source, 0, source.size());
            } catch (Exception ex) {
                System.err.println("Unable to copy file: " + f.getAbsolutePath());
                ex.printStackTrace();
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Process a given class.
     *
     * @param name
     * @param is
     * @param outputDir
     */
    private void processClass(String name, InputStream is, File outputDir) {
        try {
            FileOutputStream fos = new FileOutputStream(outputDir.getPath() + "/" + name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            lastInstrumentedClass = outputDir.getPath() + "/" + name;
            byte[] c = instrumentClass(outputDir.getAbsolutePath(), is, true);
            bos.write(c);
            bos.writeTo(fos);
            fos.close();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Process a zip file.
     *
     * @param f
     * @param outputDir
     */
    private void processZip(File f, File outputDir) {
        try {
            ZipFile zip = new ZipFile(f);
            ZipOutputStream zos = null;
            zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + "/" + f.getName()));
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().endsWith(".class")) {
                    {
                        ZipEntry outEntry = new ZipEntry(e.getName());
                        zos.putNextEntry(outEntry);

                        byte[] clazz = instrumentClass(f.getAbsolutePath(), zip.getInputStream(e), true);
                        if (clazz == null) {
                            InputStream is = zip.getInputStream(e);
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int count = is.read(buffer);
                                if (count == -1)
                                    break;
                                zos.write(buffer, 0, count);
                            }
                        } else
                            zos.write(clazz);
                        zos.closeEntry();
                    }
                } else if (e.getName().endsWith(".jar")) {
                    ZipEntry outEntry = new ZipEntry(e.getName());
                    File tmp = new File("/tmp/classfile");
                    if (tmp.exists())
                        tmp.delete();
                    FileOutputStream fos = new FileOutputStream(tmp);
                    byte buf[] = new byte[1024];
                    int len;
                    InputStream is = zip.getInputStream(e);
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                    is.close();
                    fos.close();

                    File tmp2 = new File("tmp2");
                    if (!tmp2.exists())
                        tmp2.mkdir();
                    processJar(tmp, new File("tmp2"));

                    zos.putNextEntry(outEntry);
                    is = new FileInputStream("tmp2/classfile");
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int count = is.read(buffer);
                        if (count == -1)
                            break;
                        zos.write(buffer, 0, count);
                    }
                    is.close();
                    zos.closeEntry();
                } else {
                    ZipEntry outEntry = new ZipEntry(e.getName());
                    if (e.isDirectory()) {
                        try {
                            zos.putNextEntry(outEntry);
                            zos.closeEntry();
                        } catch (ZipException exxxx) {
                            System.out.println("Ignoring exception: " + exxxx.getMessage());
                        }
                    } else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
                        // don't copy this
                    } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
                        Scanner s = new Scanner(zip.getInputStream(e));
                        zos.putNextEntry(outEntry);
                        String curPair = "";
                        while (s.hasNextLine()) {
                            String line = s.nextLine();
                            if (line.equals("")) {
                                curPair += "\n";
                                if (!curPair.contains("SHA1-Digest:"))
                                    zos.write(curPair.getBytes());
                                curPair = "";
                            } else {
                                curPair += line + "\n";
                            }
                        }
                        s.close();
                        zos.write("\n".getBytes());
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(outEntry);
                        InputStream is = zip.getInputStream(e);
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int count = is.read(buffer);
                            if (count == -1)
                                break;
                            zos.write(buffer, 0, count);
                        }
                        zos.closeEntry();
                    }
                }
            }
            zos.close();
            zip.close();
        } catch (Exception e) {
            System.err.println("Unable to process zip: " + f.getAbsolutePath());
            e.printStackTrace();
            File dest = new File(outputDir.getPath() + "/" + f.getName());
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(f).getChannel();
                destination = new FileOutputStream(dest).getChannel();
                destination.transferFrom(source, 0, source.size());
            } catch (Exception ex) {
                System.err.println("Unable to copy zip: " + f.getAbsolutePath());
                ex.printStackTrace();
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
    }

    public int getNbClassesInstrumented() {
        return nbClassesInstrumented;
    }
}
