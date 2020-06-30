package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JCovReportReaderTest
{

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void getUsedClassesAndMethods() throws ParserConfigurationException, IOException, SAXException
    {
        JCovReportReader r = new JCovReportReader();
        final File xmlJacocoReport = new File("src/test/resources/coverage/result_jcov.xml");
        UsageAnalysis observed = r.getUsedClassesAndMethods(xmlJacocoReport);
        System.out.println(observed.toString());
        assertEquals(2, observed.getAnalysis().size());
        assertEquals(3, observed.getAnalysis().get("calc/Calc").size());
        assertEquals(true, observed.getAnalysis().containsKey("calc/Main"));
    }
}
