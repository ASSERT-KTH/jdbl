package se.kth.castor.jdbl.app.wrapper;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JacocoReportReaderTest {

	@Test
	void getUsedClassesAndMethods() throws ParserConfigurationException, IOException, SAXException {
		JacocoReportReader r = new JacocoReportReader();
		Map<String, Set<String>> observed = r.getUsedClassesAndMethods(new File(JacocoReportReaderTest.class.getClassLoader().getResource("coverage/report.xml").getFile()));

		assertEquals(3, observed.size());
		assertEquals(4, observed.get("org/dummy/pa/FullyCovered").size());
		assertEquals(2, observed.get("org/dummy/pa/PartiallyCovered").size());
		assertEquals(0, observed.get("org/dummy/pa/NotCovered").size());
		assertEquals(false, observed.containsKey("org/dummy/pa/Missing"));
	}
}