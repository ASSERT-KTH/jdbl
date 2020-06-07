package se.kth.castor.jdbl.app.coverage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JacocoReportReaderTest
{

    @Test
    void getUsedClassesAndMethods() throws ParserConfigurationException, IOException, SAXException
    {
        JacocoReportReader r = new JacocoReportReader();
        final File xmlJacocoReport = new File("src/test/resources/coverage/report.xml");
        Map<String, Set<String>> observed = r.getUsedClassesAndMethods(xmlJacocoReport);
        assertEquals(3, observed.size());
        assertEquals(4, observed.get("org/dummy/pa/FullyCovered").size());
        assertEquals(2, observed.get("org/dummy/pa/PartiallyCovered").size());
        assertEquals(0, observed.get("org/dummy/pa/NotCovered").size());
        assertEquals(false, observed.containsKey("org/dummy/pa/Missing"));
    }
}
