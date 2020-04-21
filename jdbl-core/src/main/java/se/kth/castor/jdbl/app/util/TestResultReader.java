package se.kth.castor.jdbl.app.util;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class TestResultReader {
    private String path;

    public TestResultReader(String path) {
        this.path = path;
    }

    public TSResult getResults() {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        File file = new File(path + "/target/surefire-reports");
        if (!file.exists()) {
            file = new File(path);
        }
        if (!file.exists()) {
            return null;
        }
        int nbErrors = 0;
        int nbFailure = 0;
        int nbTest = 0;
        int nbSkip = 0;
        for (File f : file.listFiles()) {
            if (f.getName().endsWith(".xml")) {
                try {
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(f);
                    doc.getDocumentElement().normalize();
                    String errors = doc.getDocumentElement().getAttribute("errors");
                    if (errors != null) {
                        nbErrors += Integer.parseInt(errors);
                    }
                    String failures = doc.getDocumentElement().getAttribute("failures");
                    if (failures == null) {
                        failures = doc.getDocumentElement().getAttribute("failed");
                    }
                    if (failures != null) {
                        nbFailure += Integer.parseInt(failures);
                    }
                    String tests = doc.getDocumentElement().getAttribute("tests");
                    if (tests != null) {
                        nbTest += Integer.parseInt(tests);
                    }
                    String skipped = doc.getDocumentElement().getAttribute("skipped");
                    if (skipped != null) {
                        nbSkip += Integer.parseInt(skipped);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return new TSResult(nbTest, nbFailure, nbErrors, nbSkip);
    }
}
