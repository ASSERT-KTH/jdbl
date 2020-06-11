package util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import se.kth.castor.jdbl.app.util.MyFileUtils;

public class MyFileUtilsTest
{
    Set<String> exclusionSet;
    Set<String> classesUsed;
    File reportFile;
    File inputDir;
    File outputDir;

    @Before
    public void setupFiles()
    {
        exclusionSet = new HashSet<>();
        classesUsed = new HashSet<>(Arrays.asList("calc.MainCalculator", "calc.Calculator"));
        reportFile = new File("src/test/resources/classes/output/report.csv");
        inputDir = new File("src/test/resources/classes");
        outputDir = new File("src/test/resources/classes/output");
    }

    @Test
    public void deleteUnusedClasses() throws IOException
    {
        org.apache.commons.io.FileUtils.copyDirectory(inputDir, outputDir);
        MyFileUtils myFileUtils = new MyFileUtils(outputDir.getAbsolutePath(), exclusionSet, classesUsed, reportFile, null);
        myFileUtils.deleteUnusedClasses(outputDir.getAbsolutePath());
        assertTrue(reportFile.exists());
        assertEquals(5, new File(outputDir.getAbsolutePath() + "/calc").listFiles().length);
    }

    @After
    public void restoreFiles() throws IOException
    {
        org.apache.commons.io.FileUtils.deleteDirectory(outputDir);
    }
}
