package se.kth.castor.jdbl.coverage;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class JCovReportReader
{
    private UsageAnalysis usageAnalysis;
    private final DocumentBuilder dBuilder;

    public JCovReportReader() throws ParserConfigurationException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        dBuilder = dbFactory.newDocumentBuilder();
        // Ignore the lack of DTD
        dBuilder.setEntityResolver((publicId, systemId) -> {
            if (systemId.contains(".dtd")) {
                return new InputSource(new StringReader(""));
            } else {
                return null;
            }
        });
    }

    /**
     * Return a collection of used classes and used methods organized as following:
     * Map: class fullyQualifiedName -> Set< used method qualifier > if the class contains covered methods
     * method qualifier = methodSimpleName + descriptor
     * descriptor = (paramTypes;*)returnType
     * ex: method "contains(Ljava/lang/Object;)Z" is named contains and take an object as parameter and return a boolean
     */
    public UsageAnalysis getUsedClassesAndMethods(File jcovXMLReport) throws IOException, SAXException
    {
        usageAnalysis = new UsageAnalysis();
        Document doc = dBuilder.parse(jcovXMLReport);
        doc.getDocumentElement().normalize();

        NodeList packages = doc.getElementsByTagName("package");
        for (int i = 0; i < packages.getLength(); i++) {
            visitPackage(packages.item(i));
        }

        // Remove all classes that do not contain any covered method
        usageAnalysis.removeUncoveredClasses();

        return usageAnalysis;
    }

    private void visitPackage(Node p)
    {
        String packageName = p.getAttributes().getNamedItem("name").getNodeValue().replace(".", "/");
        NodeList classes = p.getChildNodes();
        for (int i = 0; i < classes.getLength(); i++) {
            Node n = classes.item(i);
            if (n.getNodeName().equals("class")) {
                visitClass(n, packageName);
            }
        }
    }

    private void visitClass(Node c, String packageName)
    {
        NodeList methods = c.getChildNodes();

        // interface have no child nodes, and we ignore them (coverage does not make sense)
        if (methods.getLength() == 0) {
            return;
        }
        String className = c.getAttributes().getNamedItem("name").getNodeValue();
        usageAnalysis.addEntry(packageName + "/" + className, new HashSet<>());
        for (int i = 0; i < methods.getLength(); i++) {
            Node n = methods.item(i);
            if (!n.getNodeName().equals("meth")) {
                continue;
            }
            visitMethod(n, packageName);
        }
    }

    private void visitMethod(Node m, String packageName)
    {
        if (!isCovered(m, "methenter")) {
            return;
        }
        String desc = m.getAttributes().getNamedItem("name").getNodeValue() +
            m.getAttributes().getNamedItem("vmsig").getNodeValue();

        // we add the method only if it is covered
        String className = m.getParentNode().getAttributes().getNamedItem("name").getNodeValue();
        usageAnalysis.methods(packageName + "/" + className).add(desc);
    }

    private boolean isCovered(Node c, String entity)
    {
        // we look for a child node like <counter type="entity" ... covered="?"> if ? equals "0" it is not covered,
        // otherwise it is
        NodeList counters = c.getChildNodes();

        for (int i = 0; i < counters.getLength(); i++) {
            Node n = counters.item(i);
            if (!n.getNodeName().equals("bl")) {
                continue;
            }

            NodeList blList = n.getChildNodes();
            Node nodeCounter = null;
            for (int j = 0; j < blList.getLength(); ++j) {
                Node node = blList.item(j);
                if (node.getNodeName().equals(entity)) {
                    nodeCounter = node.getAttributes().getNamedItem("count");
                    break;
                }
            }
            if (nodeCounter == null) {
                continue;
            } else {
                return !nodeCounter.getNodeValue().equals("0");
            }
        }
        return true;
    }
}
