package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacocoReportReaderTest
{
    @Test
    public void getUsedClassesAndMethods() throws ParserConfigurationException, IOException, SAXException
    {
        JacocoReportReader r = null;
        r = new JacocoReportReader();
        final File xmlJacocoReport = new File("src/test/resources/coverage/report_jacoco.xml");
        UsageAnalysis observed = null;
        observed = r.getUsedClassesAndMethods(xmlJacocoReport);
        assertEquals(2, observed.getAnalysis().size());
        assertEquals(4, observed.getAnalysis().get("org/dummy/pa/FullyCovered").size());
        assertEquals(2, observed.getAnalysis().get("org/dummy/pa/PartiallyCovered").size());
        assertEquals(false, observed.getAnalysis().containsKey("org/dummy/pa/Missing"));
    }
}
