package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.tdk.jcov.Grabber;
import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.JCov;
import com.sun.tdk.jcov.Merger;
import com.sun.tdk.jcov.RepGen;
import com.sun.tdk.jcov.data.Result;
import se.kth.castor.jdbl.util.JarUtils;
import se.kth.castor.jdbl.util.MavenUtils;

public class JCovCoverageTest
{
    private Instr instr;
    private RepGen repgen;
    private File template;
    private Merger merger;
    private File classesDir;
    private File instrumentedDir;

    @Before
    public void setUp() throws Exception
    {
        instr = new Instr();
        repgen = new RepGen();
        merger = new Merger();
        template = new File("src/test/resources/calc/template.xml");
        classesDir = new File("src/test/resources/calc/");
        instrumentedDir = new File("src/test/resources/calc/inst/");
    }

    @Test
    /**
     * java -jar jcov.jar tmplgen -t template.xml $path_to_classes
     * java -jar jcov.jar grabber -t template.xml -o result.xml
     * run the tests adding javaagent option: "-javaagent:jcov.jar=grabber"
     * java -jar jcov.jar grabbermanager -kill
     * java -jar jcov.jar repgen -o report result.xml
     *
     */
    public void instrument() throws IOException
    {
        // JCov jCov = new JCov();
        // JCov.main(new String[]{"tmplgen", "t", "template.xml", classesDir.getAbsolutePath()});
        // JCov.printHelp();
        // jCov.run(new String[]{"tmplgen", "t", "template.xml", classesDir.getAbsolutePath()});
        // jCov.run(new String[]{"grabber", "-t", "template.xml", "-o", "result.xml"});

        Grabber grabber = new Grabber();
        grabber.createServer();
        grabber.setOutputFilename("myreport.xml");
        grabber.startServer();

        grabber.stopServer(true);




        // template.getParentFile().mkdirs();
        // instr.config(true, true, true, null, null);
        // instr.instrumentFiles(new File[]{classesDir}, instrumentedDir, null);
        // instr.finishWork(template.getPath());
        //
        // MavenUtils mavenUtils = new MavenUtils(null, null);
        // mavenUtils.copyDependency("com.sun.tdk:jcov:1.0", instrumentedDir.getAbsolutePath());
        // JarUtils.decompressJars(instrumentedDir.getAbsolutePath());
        //
        //
        // final Result result = new Result(template.getAbsolutePath());
        // try {
        //     repgen.generateReport("src/test/resources/calc/inst/jcov-reports", result);
        // } catch (final Exception e) {
        // }

    }

    @After
    public void tearDown() throws Exception
    {
    }


}
