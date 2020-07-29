package se.kth.castor.jdbl.util;

//Import all needed packages

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils
{
    public static void pack(String sourceDirPath, String zipFilePath) throws IOException
    {
        Path p = Files.createFile(Paths.get(zipFilePath));
        Path pp = Paths.get(sourceDirPath);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
             Stream<Path> paths = Files.walk(pp)) {
            paths
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                });
        }
    }
}
