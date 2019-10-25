package se.kth.jdbl.wrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JacocoReportReader {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private Map<String, Set<String>> unusedClassesAndMethods;
    private DocumentBuilder dBuilder;

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public JacocoReportReader() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dBuilder = dbFactory.newDocumentBuilder();

        //Ignore the lack of DTD
        dBuilder.setEntityResolver((publicId, systemId) -> {
            if (systemId.contains(".dtd")) {
                return new InputSource(new StringReader(""));
            } else {
                return null;
            }
        });
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    /**
     * Return a collection of unused classes and unused methods organized as following:
     * Map: class fullyQualifiedName -> Set< unused method qualifier > if the class contains covered methods
     * -> null if no methods is covered
     * method qualifier = methodSimpleName + descriptor
     * descriptor = (paramTypes;*)returnType
     * ex: method "contains(Ljava/lang/Object;)Z" is named contains and take an object as parameter and return a boolean
     */
    public Map<String, Set<String>> getUnusedClassesAndMethods(File xmlJacocoReport) throws IOException, SAXException {
        unusedClassesAndMethods = new HashMap<>();
        Document doc = dBuilder.parse(xmlJacocoReport);
        doc.getDocumentElement().normalize();

        NodeList packages = doc.getElementsByTagName("package");
        for (int i = 0; i < packages.getLength(); i++) {
            visitPackage(packages.item(i));
        }

        return unusedClassesAndMethods;
    }

    //--------------------------------/
    //------ PRIVATE METHOD/S -------/
    //------------------------------/

    private void visitPackage(Node p) {
        NodeList classes = p.getChildNodes();
        for (int i = 0; i < classes.getLength(); i++) {
            Node n = classes.item(i);
            if (!n.getNodeName().equals("class")) continue;
            visitClass(n);
        }
    }

    private void visitClass(Node c) {
        // if the class is not covered at all we add is and put null is the set of uncovered method
        if (!isCovered(c, "CLASS"))
            unusedClassesAndMethods.put(c.getAttributes().getNamedItem("name").getNodeValue(), null);
        else {
            NodeList methods = c.getChildNodes();

            // interface have no child nodes, and we ignore them (coverage does not make sense)
            if (methods.getLength() == 0) return;
            unusedClassesAndMethods.put(c.getAttributes().getNamedItem("name").getNodeValue(), new HashSet<>());
            for (int i = 0; i < methods.getLength(); i++) {
                Node n = methods.item(i);
                if (!n.getNodeName().equals("method")) continue;
                visitMethod(n);
            }
        }
    }

    private boolean isCovered(Node c, String entity) {
        // we look for a child node like <counter type="entity" ... covered="?"> if ? equals "0" it is not covered, otherwise it is
        NodeList counters = c.getChildNodes();
        for (int i = 0; i < counters.getLength(); i++) {
            Node n = counters.item(i);
            if (!n.getNodeName().equals("counter")) {
                continue;
            }
            Node type = n.getAttributes().getNamedItem("type");
            if (type == null) {
                continue;
            } else if (!type.getNodeValue().equals(entity)) {
                continue;
            } else {
                return !n.getAttributes().getNamedItem("covered").getNodeValue().equals("0");
            }
        }
        return true;
    }

    private void visitMethod(Node m) {
        if (isCovered(m, "METHOD")) return;
        String desc = m.getAttributes().getNamedItem("name").getNodeValue() + m.getAttributes().getNamedItem("desc").getNodeValue();

        // we add the method only if it is not covered
        unusedClassesAndMethods.get(m.getParentNode().getAttributes().getNamedItem("name").getNodeValue()).add(desc);
    }
}
