package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.maven.project.MavenProject;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.Merger;
import com.sun.tdk.jcov.RepGen;
import com.sun.tdk.jcov.data.Result;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

/**
 * @https://wiki.openjdk.java.net/display/CodeTools/JCov+FAQ
 */
public class JCovCoverage extends AbstractCoverage
{
    private final Instr instr;
    private final RepGen repgen;
    private File template;
    private Merger merger;

    public JCovCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
        instr = new Instr();
        repgen = new RepGen();
        merger = new Merger();
        template = new File(mavenProject.getBasedir().getAbsolutePath() + "/target/instrumented/template.xml");
        LOGGER = LogManager.getLogger(JCovCoverage.class.getName());
    }

    @Override
    protected UsageAnalysis executeTestBasedAnalysis()
    {
        writeCoverage();
        UsageAnalysis usageAnalysis = new UsageAnalysis();
        return usageAnalysis;
    }

    @Override
    protected UsageAnalysis executeEntryPointAnalysis()
    {
        // TODO implement the entry point approach
        LOGGER.info("Output directory: " + mavenProject.getBuild().getOutputDirectory());
        LOGGER.info("entryClass: " + entryClass);
        LOGGER.info("entryMethod: " + entryMethod);
        LOGGER.info("entryParameters: " + entryParameters);
        return null;
    }

    @Override
    protected UsageAnalysis executeConservativeAnalysis()
    {
        // TODO implement the conservative approach
        return null;
    }

    @Override
    protected void writeCoverage()
    {
        LOGGER.info("Running JCov");
        final String baseDir = mavenProject.getBasedir().getAbsolutePath();
        final String classesDir = baseDir + "/target/classes";
        final String testDir = baseDir + "/target/test-classes";
        final String instrumentedDir = baseDir + "/target/instrumented";
        final String classesOriginalDir = baseDir + "/target/classes-original";

        MavenUtils mavenUtils = copyDependencies(classesDir, testDir);
        instrument(classesDir, instrumentedDir);
        replaceInstrumentedClasses(classesDir, instrumentedDir, classesOriginalDir);
        addJCovAsTestDependency(testDir, mavenUtils);


        try {
            TestRunner.runTests2(mavenProject, classesDir + "/template.xml");
        } catch (IOException e) {
        }

        writeReports(baseDir);
        restoreOriginalClasses(classesDir, classesOriginalDir);
    }

    private void writeReports(String basedir)
    {
        final Result result = new Result(basedir + "/result.xml");
        try {
            LOGGER.info("Generating JCov report");
            repgen.generateReport(basedir + "/target/jcov-reports", result);
        } catch (final Exception e) {
            LOGGER.error("Error processing coverage file");
        }
    }

    /**
     * The instrumented classes need JCov to compile with the inserted probes.
     */
    private void addJCovAsTestDependency(final String testDir, MavenUtils mavenUtils)
    {
        mavenUtils.copyDependency("com.sun.tdk:jcov-file-saver:1.0", testDir);
        JarUtils.decompressJars(testDir);
    }

    /**
     * Instrument classes with JCov.
     */
    private void instrument(final String classesDir, final String instrumentedDir)
    {
        LOGGER.info("Instrumenting classes in " + classesDir + ", outputting to " + instrumentedDir);
        template.getParentFile().mkdirs();
        try {
            // Instrument abstracts, fields, and natives
            instr.config(true, true, true, null, null);
            instr.instrumentFiles(new File[]{new File(classesDir)}, new File(instrumentedDir), null);
        } catch (IOException e) {
            LOGGER.error("IO error while instrumenting classes", e);
        }
        LOGGER.info("Generated template at " + template.getPath());
        instr.finishWork(template.getPath());
    }
}
