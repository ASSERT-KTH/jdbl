package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.xml.XMLFormatter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import se.kth.castor.jdbl.debloat.DebloatTypeEnum;
import se.kth.castor.jdbl.test.TestRunner;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

/**
 * This class runs JaCoCo via CLI.
 *
 * @see <a https://www.jacoco.org/jacoco/trunk/doc/cli.html</a>
 */
public class JacocoCoverage extends AbstractCoverage implements UsageAnalyzer
{
    private static final Logger LOGGER = LogManager.getLogger(JacocoCoverage.class.getName());

    private Instrumenter instrumenter;
    private File dest;
    private List<File> source;
    private List<File> execFiles;
    private List<File> classFiles;
    private List<File> sourceFiles;
    private String name = "JaCoCo Coverage Report";
    private File xml;
    JacocoReportReader reportReader = null;

    public JacocoCoverage(
        MavenProject mavenProject,
        File mavenHome,
        DebloatTypeEnum debloatTypeEnum)
    {
        super(mavenProject, mavenHome, debloatTypeEnum);
    }

    @Override
    public UsageAnalysis analyzeUsages()
    {
        try {
            writeCoverage();
        } catch (Exception e) {
            LOGGER.error("Error writing coverage file.");
        }

        File report = new File(mavenProject.getBasedir().getAbsolutePath() + "/target/report.xml");

        // Read the jacoco report
        try {
            reportReader = new JacocoReportReader();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Error parsing jacoco.xml file.");
        }
        try {
            assert reportReader != null;
            return reportReader.getUsedClassesAndMethods(report);
        } catch (IOException | SAXException e) {
            LOGGER.error("Error getting unused classes and methods file.");
        }

        return null;
    }

    private void writeCoverage() throws IOException
    {
        LOGGER.info("Running JaCoCo");
        final String baseDir = mavenProject.getBasedir().getAbsolutePath();
        final String classesDir = baseDir + "/target/classes";
        final String testDir = baseDir + "/target/test-classes";
        final String instrumentedDir = baseDir + "/target/instrumented";
        final String classesOriginalDir = baseDir + "/target/classes-original";

        source = Arrays.asList(new File(classesDir));
        dest = new File(instrumentedDir);
        execFiles = Arrays.asList(new File(baseDir + "/jacoco.exec"));
        classFiles = Arrays.asList(new File(classesDir));
        sourceFiles = Arrays.asList(new File(mavenProject.getBasedir() + "/src/main/java"));
        xml = new File(baseDir + "/target/report.xml");

        MavenUtils mavenUtils = copyDependencies(classesDir);
        deleteNonClassFiles(classesDir);
        executeInstrument();
        replaceInstrumentedClasses(classesDir, instrumentedDir, classesOriginalDir);
        addJaCoCoAsTestDependency(testDir, mavenUtils);
        runTests();
        restoreOriginalClasses(classesDir, classesOriginalDir);
        executeReport();
    }

    public int executeReport() throws IOException
    {
        final ExecFileLoader loader = loadExecutionData();
        final IBundleCoverage bundle = analyze(loader.getExecutionDataStore());
        writeReports(bundle, loader);
        return 0;
    }

    private ExecFileLoader loadExecutionData() throws IOException
    {
        final ExecFileLoader loader = new ExecFileLoader();
        if (execFiles.isEmpty()) {
            LOGGER.warn("No execution data files provided.");
        } else {
            for (final File file : execFiles) {
                LOGGER.info("Loading execution data file " + file.getAbsolutePath());
                loader.load(file);
            }
        }
        return loader;
    }

    private IBundleCoverage analyze(final ExecutionDataStore data) throws IOException
    {
        final CoverageBuilder builder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(data, builder);
        for (final File f : classFiles) {
            analyzer.analyzeAll(f);
        }
        printNoMatchWarning(builder.getNoMatchClasses());
        return builder.getBundle(name);
    }

    private void printNoMatchWarning(final Collection<IClassCoverage> nomatch)
    {
        if (!nomatch.isEmpty()) {
            LOGGER.warn("Some classes do not match with execution data.");
            LOGGER.warn("For report generation the same class files must be used as at runtime.");
            for (final IClassCoverage c : nomatch) {
                LOGGER.warn("Execution data for class" + c.getName() + "does not match.");
            }
        }
    }

    private void writeReports(final IBundleCoverage bundle, final ExecFileLoader loader) throws IOException
    {
        LOGGER.info("Analyzing " + Integer.valueOf(bundle.getClassCounter().getTotalCount()) + " classes");
        final IReportVisitor visitor = createReportVisitor();
        visitor.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
        visitor.visitBundle(bundle, getSourceLocator());
        visitor.visitEnd();
    }

    private IReportVisitor createReportVisitor() throws IOException
    {
        final List<IReportVisitor> visitors = new ArrayList<>();
        final XMLFormatter formatter = new XMLFormatter();
        visitors.add(formatter.createVisitor(new FileOutputStream(xml)));
        return new MultiReportVisitor(visitors);
    }

    private ISourceFileLocator getSourceLocator()
    {
        int tabwidth = 4;
        final MultiSourceFileLocator multi = new MultiSourceFileLocator(tabwidth);
        for (final File f : sourceFiles) {
            multi.add(new DirectorySourceFileLocator(f, null, tabwidth));
        }
        return multi;
    }

    private void executeInstrument() throws IOException
    {
        final File absoluteDest = dest.getAbsoluteFile();
        instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        int total = 0;
        for (final File s : source) {
            if (s.isFile()) {
                total += instrument(s, new File(absoluteDest, s.getName()));
            } else {
                total += instrumentRecursive(s, absoluteDest);
            }
        }
        LOGGER.info(Integer.valueOf(total) + " classes instrumented to " + absoluteDest);
    }

    private int instrumentRecursive(final File src, final File dest) throws IOException
    {
        int total = 0;
        if (src.isDirectory()) {
            for (final File child : src.listFiles()) {
                total += instrumentRecursive(child, new File(dest, child.getName()));
            }
        } else {
            total += instrument(src, dest);
        }
        return total;
    }

    private int instrument(final File src, final File dest) throws IOException
    {
        dest.getParentFile().mkdirs();
        try (InputStream input = new FileInputStream(src)) {
            try (OutputStream output = new FileOutputStream(dest)) {
                return instrumenter.instrumentAll(input, output,
                    src.getAbsolutePath());
            }
        } catch (final IOException e) {
            Files.delete(dest.toPath());
            throw e;
        }
    }

    /**
     * Restore the (previously replaced) original classes with the original non-instrumented classes.
     */
    private void restoreOriginalClasses(String classesDir, String classesOriginalDir)
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
    private void replaceInstrumentedClasses(String classesDir, String instrumentedDir, String classesOriginalDir)
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

    /**
     * The instrumented classes need JaCoCo to compile with the inserted probes.
     */
    private void addJaCoCoAsTestDependency(String testDir, MavenUtils mavenUtils)
    {
        mavenUtils.copyDependency("org.jacoco:org.jacoco.agent:0.8.5", testDir);
        JarUtils.decompressJars(testDir);
    }

    private void runTests()
    {
        try {
            TestRunner.runTests(mavenProject);
        } catch (IOException e) {
            LOGGER.error("Error running the tests.");
        }
    }

    /**
     * Delete non class files to avoid wrong instrumentation attempts (e.g., resources).
     */
    private void deleteNonClassFiles(String classesDir)
    {
        File directory = new File(classesDir + "/META-INF");
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Error deleting directory " + directory.getName());
        }
    }

    /**
     * Copy dependencies to be instrumented by JaCoCo
     */
    private MavenUtils copyDependencies(String classesDir)
    {
        MavenUtils mavenUtils = new MavenUtils(super.mavenHome, mavenProject.getBasedir());
        mavenUtils.copyDependencies(classesDir);
        JarUtils.decompressJars(classesDir);
        return mavenUtils;
    }
}
