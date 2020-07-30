package se.kth.castor.jdbl.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import se.kth.castor.jdbl.coverage.CoverageToolEnum;
import se.kth.castor.jdbl.coverage.JCovCoverage;
import se.kth.castor.jdbl.coverage.JVMClassCoverage;
import se.kth.castor.jdbl.coverage.JVMClassesCoveredSingleton;
import se.kth.castor.jdbl.coverage.JacocoCoverage;
import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.coverage.YajtaCoverage;
import se.kth.castor.jdbl.debloat.AbstractMethodDebloat;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.debloat.TestBasedMethodDebloat;
import se.kth.castor.jdbl.test.StackLine;
import se.kth.castor.jdbl.test.TestResultReader;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.MyFileUtils;
import se.kth.castor.jdbl.util.MyFileWriter;
import se.kth.castor.jdbl.util.ZipUtils;

/**
 * This Mojo debloats the project according to the coverage of its test suite.
 * Probes are inserted in order to keep track of the classes and methods used.
 * Non covered classes are removed from the final jar file, the non covered
 * methods are replaced by an <code>UnsupportedOperationException</code>.
 */
@Mojo(name = "test-based-debloat", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class TestBasedDebloatMojo extends AbstractDebloatMojo
{
    @Override
    public void doExecute()
    {
        printCustomStringToConsole("JDBL: STARTING TEST BASED DEBLOAT");
        Instant start = Instant.now();
        String outputDirectory = getProject().getBuild().getOutputDirectory();
        String projectBaseDir = getProject().getBasedir().getAbsolutePath();
        MyFileWriter myFileWriter = new MyFileWriter(projectBaseDir);
        myFileWriter.resetJDBLReportsDirectory();

        // Run JCov usage analysis
        JCovCoverage jcovCoverage = new JCovCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jcovUsageAnalysis = jcovCoverage.analyzeUsages();

        // Run yajta usage analysis
        YajtaCoverage yajtaCoverage = new YajtaCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis yajtaUsageAnalysis = yajtaCoverage.analyzeUsages();

        // Run JaCoCo usage analysis
        JacocoCoverage jacocoCoverage = new JacocoCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jacocoUsageAnalysis = jacocoCoverage.analyzeUsages();

        // Run JVM class usage analysis
        JVMClassCoverage jvmClassCoverage = new JVMClassCoverage(getProject(), mavenHome, DebloatTypeEnum.TEST_DEBLOAT);
        UsageAnalysis jvmUsageAnalysis = jvmClassCoverage.analyzeUsages();

        // Print out JCov coverage output
        System.out.println("JCov:");
        if (!jcovUsageAnalysis.classes().isEmpty() && yajtaUsageAnalysis != null) {
            System.out.print(jcovUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JCOV, jcovUsageAnalysis);
        printCoverageAnalysisResults(jcovUsageAnalysis);

        // Print out Yajta coverage output
        System.out.println("Yajta:");
        if (!yajtaUsageAnalysis.classes().isEmpty() && yajtaUsageAnalysis != null) {
            System.out.print(yajtaUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.YAJTA, yajtaUsageAnalysis);
        printCoverageAnalysisResults(yajtaUsageAnalysis);

        // Print out JaCoCo coverage output
        System.out.println("JaCoCo:");
        if (!jacocoUsageAnalysis.classes().isEmpty() && jacocoUsageAnalysis != null) {
            System.out.print(jacocoUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JACOCO, jacocoUsageAnalysis);
        printCoverageAnalysisResults(jacocoUsageAnalysis);

        // Print out JVM coverage output
        System.out.println("JVM:");
        if (!jvmUsageAnalysis.classes().isEmpty() && jvmUsageAnalysis != null) {
            System.out.print(jvmUsageAnalysis.toString());
        }
        myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JVM_CLASS_LOADER, jvmUsageAnalysis);
        printCoverageAnalysisResults(jvmUsageAnalysis);

        // Merge the coverage analysis
        UsageAnalysis mergedUsageAnalysis = yajtaUsageAnalysis.mergeWith(jacocoUsageAnalysis).mergeWith(jcovUsageAnalysis);

        // Get the classes loaded by the JVM
        Set<String> usedClasses = null;
        try {
            this.printClassesLoaded();
            usedClasses = TestBasedDebloatMojo.getUsedClasses(mergedUsageAnalysis);
        } catch (RuntimeException e) {
            this.getLog().error("Error computing JaCoCo usage analysis.");
        }

        // Read the failing methods from the stacktrace
        TestResultReader testResultReader = new TestResultReader(".");
        Set<StackLine> failingMethods = testResultReader.getMethodFromStackTrace();
        for (StackLine failingMethod : failingMethods) {
            if (usedClasses != null) {
                usedClasses.add(failingMethod.getClassName());
            }
        }

        // Write to a file with the status of classes (Used or Bloated) in each dependency
        try {
            myFileWriter.writeClassStatusPerDependency(usedClasses);
        } catch (RuntimeException e) {
            this.getLog().error("Error writing the status of classes per dependency.");
        }

        // ----------------------------------------------------
        this.getLog().info("Starting removing unused classes...");
        this.removeUnusedClasses(outputDirectory, usedClasses);

        // ----------------------------------------------------
        this.getLog().info("Starting removing unused methods...");
        this.removeUnusedMethods(mergedUsageAnalysis, failingMethods);

        // ----------------------------------------------------
        try {
            this.getLog().info("Starting running the test suite on the debloated version...");
            TestRunner.runTests(getProject(), false);
        } catch (IOException e) {
            this.getLog().error("IOException when rerunning the tests");
        }

        // ----------------------------------------------------
        myFileWriter.writeTimeElapsedReportFile(start);
        printCustomStringToConsole("JDBL: TEST BASED DEBLOAT FINISHED");
    }

    private void printClassesLoaded()
    {
        final int nbOfClassesLoaded = JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded().size();
        this.getLog().info("Loaded classes (" + nbOfClassesLoaded + ')');
        JVMClassesCoveredSingleton.INSTANCE.printClassesLoaded();
        this.getLog().info(getLineSeparator());
    }

    private void removeUnusedClasses(final String outputDirectory, final Set<String> usedClasses)
    {
        try {
            final String projectBaseDir = getProject().getBasedir().getAbsolutePath();
            MyFileUtils myFileUtils = new MyFileUtils(outputDirectory,
                new HashSet<>(),
                usedClasses,
                projectBaseDir,
                getProject().getTestClasspathElements());

            // Delete bloated classes in the jars with dependencies
            Collection<File> jarFiles = FileUtils.listFiles(new File(projectBaseDir + "/target"), new String[]{"jar"}, false);

            for (File jarFile : jarFiles) {
                if (jarFile.getName().endsWith("-jar-with-dependencies.jar")) {

                    String dirPath = projectBaseDir + "/target/" + jarFile.getName().substring(0, jarFile.getName().length() - 4);
                    getLog().info("Removing bloated classes in " + dirPath);

                    File dir = new File(dirPath);
                    dir.mkdir();


                    unzipJar(dirPath, jarFile.getAbsolutePath());
                    //
                    // JarFile jar = new JarFile(jarFile);
                    // Enumeration enumEntries = jar.entries();
                    // while (enumEntries.hasMoreElements()) {
                    //     JarEntry file = (JarEntry) enumEntries.nextElement();
                    //     File f = new File(dirPath + "/" + file.getName());
                    //     if (file.isDirectory()) { // if its a directory, create it
                    //         f.mkdir();
                    //         continue;
                    //     }
                    //     InputStream is = jar.getInputStream(file); // get the input stream
                    //     FileOutputStream fos = new FileOutputStream(f);
                    //     while (is.available() > 0) {  // write contents of 'is' to 'fos'
                    //         fos.write(is.read());
                    //     }
                    //     fos.close();
                    //     is.close();
                    // }
                    // jar.close();

                    myFileUtils.deleteUnusedClasses(dirPath, dirPath);
                }
            }

            this.getLog().info("Total classes removed: " + myFileUtils.nbClassesRemoved());
        } catch (Exception e) {
            this.getLog().error(String.format("Error deleting unused classes: %s", e));
        }
    }

    private void unzipJar(String destinationDir, String jarPath) throws IOException {
        File file = new File(jarPath);
        JarFile jar = new JarFile(file);

        // fist get all directories,
        // then make those directory on the destination Path
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
            JarEntry entry = (JarEntry) enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);

            if (fileName.endsWith("/")) {
                f.mkdirs();
            }

        }

        //now create all files
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
            JarEntry entry = (JarEntry) enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);

            if (!fileName.endsWith("/")) {
                InputStream is = jar.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);

                // write contents of 'is' to 'fos'
                while (is.available() > 0) {
                    fos.write(is.read());
                }

                fos.close();
                is.close();
            }
        }
    }

    private void removeUnusedMethods(final UsageAnalysis usageAnalysis,
        Set<StackLine> failingMethods)
    {

        Collection<File> jarFiles = FileUtils.listFiles(new File(getProject().getBasedir().getAbsolutePath() + "/target"),
            new String[]{"jar"}, false);

        for (File jarFile : jarFiles) {
            if (jarFile.getName().endsWith("-jar-with-dependencies.jar")) {

                String dirPath = getProject().getBasedir().getAbsolutePath() + "/target/" +
                    jarFile.getName().substring(0, jarFile.getName().length() - 4);

                getLog().info("Removing bloated methods in " + dirPath);
                AbstractMethodDebloat testBasedMethodDebloat = new TestBasedMethodDebloat(dirPath,
                    usageAnalysis,
                    getProject().getBasedir().getAbsolutePath(), failingMethods);
                try {
                    testBasedMethodDebloat.removeUnusedMethods();
                    this.getLog().info("Total methods in used classes removed: " + testBasedMethodDebloat.nbMethodsRemoved());
                } catch (IOException e) {
                    this.getLog().error(String.format("Error: %s", e));
                }

                try {
                    final String zipJar = getProject().getBasedir().getAbsolutePath() + "/.jdbl/" +
                        jarFile.getName().substring(0, jarFile.getName().length() - 4) + "-debloated.jar";

                    getLog().info("Moving debloated jar to " + zipJar);
                    ZipUtils.pack(dirPath, zipJar);


                    final String pathForOriginalJar = getProject().getBasedir().getAbsolutePath() + "/.jdbl/" +
                        jarFile.getName().substring(0, jarFile.getName().length() - 4) + "-original.jar";
                    getLog().info("Moving original jar to " + pathForOriginalJar);
                    FileUtils.copyFile(jarFile, new File(pathForOriginalJar));
                } catch (IOException e) {
                    getLog().error("Error packing the debloated JAR.");
                }

                // Copy debloated
                try {
                    final String classesDir = getProject().getBuild().getOutputDirectory();
                    FileUtils.deleteDirectory(new File(classesDir));
                    FileUtils.moveDirectory(new File(dirPath), new File(classesDir));
                } catch (IOException e) {
                    getLog().error("Error rolling back the compiled classes.");
                }
            }
        }
    }

    private static Set<String> getUsedClasses(final UsageAnalysis usageAnalysis)
    {
        // get the union of the JaCoCo output and the JVM class loader results
        Set<String> usedClasses = new HashSet<>(JVMClassesCoveredSingleton.INSTANCE.getClassesLoaded());
        usageAnalysis
            .getAnalysis()
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .forEach(className -> usedClasses.add(className.getKey().replace('/', '.')));
        return usedClasses;
    }

    private void printCoverageAnalysisResults(final UsageAnalysis usageAnalysis)
    {
        this.getLog().info("ANALYSIS RESULTS:");
        this.getLog().info(String.format("Total used classes: %d",
            usageAnalysis.getAnalysis().entrySet().stream().filter(e -> e.getValue() != null).count()));
        this.getLog().info(String.format("Total used methods: %d",
            usageAnalysis.getAnalysis().values().stream().filter(Objects::nonNull).mapToInt(Set::size).sum()));
        this.getLog().info(getLineSeparator());
    }
}
