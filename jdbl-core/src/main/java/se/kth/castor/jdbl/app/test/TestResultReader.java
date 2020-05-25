package se.kth.castor.jdbl.app.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class TestResultReader
{
    private String path;

    public TestResultReader(String path)
    {
        this.path = path;
    }

    private static Set<StackLine> extractError(String content) {
        Set<StackLine> output = new HashSet<>();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("at ")) {
                continue;
            }
            int index = line.indexOf('(');
            if (index == -1) {
                continue;
            }

            String classMethodName = line.substring(3, index);
            int indexMethod = classMethodName.lastIndexOf('.');
            String methodName = classMethodName.substring(indexMethod + 1);
            String className = classMethodName.substring(0, indexMethod);
            String[] position = line.substring(index + 1, line.length() - 2).split(":");
            StackLine stackLine = new StackLine(className, methodName, position[0], Integer.parseInt(position[1]));
            output.add(stackLine);
        }
        return output;
    }
    public Set<StackLine> getMethodFromStackTrace() {
        Set<StackLine> output = new HashSet<>();
        File file = new File(path + "/target/surefire-reports");
        if (!file.exists()) {
            file = new File(path);
        }
        if (!file.exists()) {
            return output;
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        for (File f : file.listFiles()) {
            if (f.getName().endsWith(".xml")) {
                try {
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(f);
                    doc.getDocumentElement().normalize();
                    NodeList errors = doc.getDocumentElement().getElementsByTagName("error");
                    for (int i = 0; i < errors.getLength(); i++) {
                        Node error = errors.item(i);
                        output.addAll(extractError(error.getTextContent()));
                    }
                    errors = doc.getDocumentElement().getElementsByTagName("rerunError");
                    for (int i = 0; i < errors.getLength(); i++) {
                        Node error = errors.item(i);
                        output.addAll(extractError(error.getTextContent()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return output;
    }

    public TestResult getResults()
    {
        File file = new File(path + "/target/surefire-reports");
        if (!file.exists()) {
            file = new File(path);
        }
        if (!file.exists()) {
            return null;
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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
        return new TestResult(nbTest, nbFailure, nbErrors, nbSkip);
    }
}
