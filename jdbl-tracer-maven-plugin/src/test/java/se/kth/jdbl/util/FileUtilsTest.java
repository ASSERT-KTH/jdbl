package se.kth.jdbl.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FileUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void deleteUntracedClasses() {
        // TODO
    }

    @Test
    public void testDeleteFolder() throws IOException {
        File dir = new File("src/main/resources/for_test_only/");
        if (!dir.mkdirs()) { // the directory was created successfully
            File file = new File("src/main/resources/for_test_only/tmp_file_for_test_only.txt");
            if (!file.exists()) { // the file was created successfully
                file.createNewFile();
            }
            // check that the file and directory were created
            Assert.assertTrue(file.exists());
            // check that the file and directory were deleted
            FileUtils.deleteDirectory(dir);
            Assert.assertFalse(dir.exists());
        }
    }

    @Test
    public void testRenameFolder() {
        File dir = new File("src/main/resources/for_test_only/");
        dir.mkdirs();
        FileUtils.renameFolder("src/main/resources/for_test_only", "src/main/resources/renamed_folder");
        File renamedFolder = new File("src/main/resources/renamed_folder");
        // check that the directory was created
        Assert.assertTrue(renamedFolder.exists());
        // remove the directory
        renamedFolder.delete();
    }

}