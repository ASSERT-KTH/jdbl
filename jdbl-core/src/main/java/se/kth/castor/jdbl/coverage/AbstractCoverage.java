package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;

import se.kth.castor.jdbl.adapter.ConstantAdapter;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.deptree.OptionalDependencyIgnorer;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

public abstract class AbstractCoverage implements UsageAnalyzer
{
    protected Logger LOGGER;
    protected MavenProject mavenProject;
    protected File mavenHome;
    protected DebloatTypeEnum debloatTypeEnum;
    protected String entryClass;
    protected String entryMethod;
    protected String entryParameters;

    /**
     * Constructor for entry-point-debloat.
     */
    public AbstractCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum,
        String entryClass, String entryMethod, String entryParameters)
    {
        this.mavenProject = mavenProject;
        this.mavenHome = mavenHome;
        this.debloatTypeEnum = debloatTypeEnum;
        this.entryClass = entryClass;
        this.entryMethod = entryMethod;
        this.entryParameters = entryParameters;
    }

    /**
     * Constructor for test-based-debloat.
     */
    public AbstractCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum)
    {
        this.mavenProject = mavenProject;
        this.mavenHome = mavenHome;
        this.debloatTypeEnum = debloatTypeEnum;
    }

    @Override
    public UsageAnalysis analyzeUsages()
    {
        switch (debloatTypeEnum) {
            case TEST_DEBLOAT:
                return executeTestBasedAnalysis();
            case ENTRY_POINT_DEBLOAT:
                return executeEntryPointAnalysis();
            case CONSERVATIVE_DEBLOAT:
                return executeConservativeAnalysis();
            default:
                return null;
        }
    }

    protected abstract UsageAnalysis executeTestBasedAnalysis();
    protected abstract UsageAnalysis executeEntryPointAnalysis();
    protected abstract UsageAnalysis executeConservativeAnalysis();

    /**
     * Dump the coverage information to a file for its analysis.
     */
    protected abstract void writeCoverage() throws IOException;

    /**
     * Execute the test suite on the instrumented bytecode.
     */
    protected void runTests()
    {
        try {
            TestRunner.runTests(mavenProject, true);
        } catch (IOException e) {
            LOGGER.error("Error running the tests.");
        }
    }

    /**
     * Copy all the dependencies (direct and transitive) to a directory to be instrumented.
     */
    protected MavenUtils copyDependencies(String classesDir, String testDir)
    {
        MavenUtils mavenUtils = new MavenUtils(mavenHome, mavenProject.getBasedir());
        mavenUtils.copyRuntimeDependencies(classesDir);
        mavenUtils.copyProvidedDependencies(testDir);
        mavenUtils.copySystemDependencies(testDir);
        excludeOptionalDependencies(mavenUtils);
        JarUtils.decompressJars(classesDir);
        JarUtils.decompressJars(testDir);
        // applyBytecodeTransformations(classesDir);
        return mavenUtils;
    }

    /**
     * Transform the bytecode to facilitate coverage of certain constructs.
     */
    private void applyBytecodeTransformations(final String classesDir)
    {
        Iterator<File> itFiles = FileUtils.iterateFiles(new File(classesDir), new String[]{"class"}, true);
        while (itFiles.hasNext()) {
            File file = itFiles.next();
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                ConstantAdapter constantAdapter = new ConstantAdapter(fileInputStream);
                byte[] result = constantAdapter.addField();
                FileUtils.forceDelete(file);
                FileUtils.writeByteArrayToFile(new File(file.getAbsolutePath()), result);
                fileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Error applying bytecode transformation.");
            }
        }
    }

    /**
     * Do not process the optional dependencies.
     */
    private void excludeOptionalDependencies(final MavenUtils mavenUtils)
    {
        OptionalDependencyIgnorer optionalDependencyIgnorer = new OptionalDependencyIgnorer(mavenProject);
        optionalDependencyIgnorer.removeOptionalDependencies(mavenUtils);
    }

    /**
     * Delete non class files to avoid wrong instrumentation attempts (e.g., when tying to instrument resources).
     */
    protected void deleteNonClassFiles(final String classesDir)
    {
        File directory = new File(classesDir + "/META-INF");
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Error deleting directory " + directory.getName());
        }
    }

    /**
     * Restore the (previously replaced) original classes with the original non-instrumented classes.
     */
    protected void restoreOriginalClasses(String classesDir, String classesOriginalDir)
    {
        try {
            FileUtils.deleteDirectory(new File(classesDir));
            FileUtils.moveDirectory(new File(classesOriginalDir), new File(classesDir));
        } catch (IOException e) {
            LOGGER.error("Error rolling back the compiled classes.");
        }
    }

    /**
     * Replace the original compiled classes with the instrumented classes.
     */
    protected void replaceInstrumentedClasses(String classesDir, String instrumentedDir, String classesOriginalDir)
    {
        LOGGER.info("Starting replacing instrumented classes.");
        try {
            FileUtils.moveDirectory(new File(classesDir),
                new File(classesOriginalDir));
            FileUtils.moveDirectory(new File(instrumentedDir),
                new File(classesDir));
        } catch (Exception e) {
            LOGGER.error("Error replacing instrumented classes with in target/classes directory.");
        }
    }
}
