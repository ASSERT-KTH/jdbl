package se.kth.castor.jdbl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;

public class DebloatedJar
{

    private String projectBaseDir;

    public DebloatedJar(final String projectBaseDir)
    {
        this.projectBaseDir = projectBaseDir;
    }

    public void create(Set<String> classesUsed) throws IOException
    {
        // Create file descriptors for the jar and a temp jar.
        Collection<File> jarFiles = FileUtils.listFiles(new File(projectBaseDir + "/target"), new String[]{"jar"}, false);

        for (File jarFile : jarFiles) {
            File tempJarFile = new File(projectBaseDir + "/.jdbl/" +
                jarFile.getName().substring(0, jarFile.getName().length() - 4) +
                "-debloated.jar");

            // Open the jar file.
            JarFile jar = new JarFile(jarFile);

            try {
                // Create a temp jar file with no manifest. (The manifest will
                // be copied when the entries are copied.)
                Manifest jarManifest = jar.getManifest();
                JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile));

                // Allocate a buffer for reading entry data.
                byte[] buffer = new byte[1024];
                int bytesRead;

                try {
                    // Loop through the jar entries and add them to the temp jar,
                    // skipping the entry that was added to the temp jar already.

                    for (Enumeration entries = jar.entries(); entries.hasMoreElements(); ) {
                        // Get the next entry.
                        JarEntry entry = (JarEntry) entries.nextElement();

                        // If the entry has not been added already, add it.
                        if (entry.getName().endsWith(".class")) {
                            if (classesUsed.contains(getBinaryName(entry.getName()))) {

                                // Get an input stream for the entry.
                                InputStream entryStream = jar.getInputStream(entry);

                                // Read the entry and write it to the temp jar.
                                tempJar.putNextEntry(entry);

                                while ((bytesRead = entryStream.read(buffer)) != -1) {
                                    tempJar.write(buffer, 0, bytesRead);
                                }
                            }
                        } else {
                            // Get an input stream for the entry.
                            InputStream entryStream = jar.getInputStream(entry);

                            // Read the entry and write it to the temp jar.
                            tempJar.putNextEntry(entry);

                            while ((bytesRead = entryStream.read(buffer)) != -1) {
                                tempJar.write(buffer, 0, bytesRead);
                            }
                        }
                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                    // Add a stub entry here, so that the jar will close without an
                    // exception.
                    tempJar.putNextEntry(new JarEntry("stub"));
                } finally {
                    tempJar.close();
                }
            } finally {
                jar.close();
                System.out.println(projectBaseDir + " closed.");
            }
        }
    }

    private String getBinaryName(final String classFilePath)
    {
        return classFilePath
            .replaceAll("/", ".")
            .substring(0, classFilePath.length() - 6);
    }
}
