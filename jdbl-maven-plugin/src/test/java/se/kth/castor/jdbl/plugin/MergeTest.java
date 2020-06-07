package se.kth.castor.jdbl.plugin;

import org.junit.Test;
import org.xml.sax.SAXException;
import se.kth.castor.jdbl.app.wrapper.JacocoReportReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MergeTest {

	@Test
	public void testCoverageReportMerging() throws ParserConfigurationException, IOException, SAXException {
		JacocoReportReader r = new JacocoReportReader();
		Map<String, Set<String>> observed = r.getUsedClassesAndMethods(new File(MergeTest.class.getClassLoader().getResource("coverage/report.xml").getFile()));

		assertEquals(3, observed.size());
		assertEquals(4, observed.get("org/dummy/pa/FullyCovered").size());
		assertEquals(2, observed.get("org/dummy/pa/PartiallyCovered").size());
		assertEquals(0, observed.get("org/dummy/pa/NotCovered").size());
		assertEquals(false, observed.containsKey("org/dummy/pa/Missing"));

		TestBasedDebloatMojo mojo = new TestBasedDebloatMojo();

		Map<String, Set<String>> merge = mojo.addYajtaAnalysis(observed, MergeTest.class.getClassLoader().getResource("coverage").getFile());

		assertEquals(4, merge.size());
		assertEquals(4, merge.get("org/dummy/pa/FullyCovered").size());
		assertEquals(4, merge.get("org/dummy/pa/PartiallyCovered").size());
		assertEquals(3, merge.get("org/dummy/pa/NotCovered").size());
		assertEquals(true, merge.containsKey("org/dummy/pa/Missing"));
		assertEquals(3, merge.get("org/dummy/pa/Missing").size());
	}

}