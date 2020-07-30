package se.kth.castor.jdbl.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class JarUtilsTest
{
    File jarFile;
    File outputDir;

    @Before
    public void setupFiles()
    {
        outputDir = new File("src/test/resources/jars/decompressed");
        jarFile = new File("src/test/resources/jars/org.jacoco.agent-0.7.9.jar");
    }

    @Test
    public void testThatAllJarFilesInFolderAreDecompressed() throws IOException
    {
        org.apache.commons.io.FileUtils.copyFileToDirectory(jarFile, outputDir);
        JarUtils.decompressJars(outputDir.getAbsolutePath());

        boolean existJarFile = false;

        for (File f : Objects.requireNonNull(outputDir.listFiles())) {
            if (f.getName().endsWith(".jar")) {
                existJarFile = true;
            }
            assertFalse(existJarFile);
        }
    }

    @After
    public void restoreFiles() throws IOException
    {
        org.apache.commons.io.FileUtils.deleteDirectory(outputDir);
    }
}
