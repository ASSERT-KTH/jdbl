package se.kth.castor.jdbl.debloat;

// import java.io.File;
// import java.io.IOException;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Set;
//
// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;
// import org.xml.sax.SAXException;
//
// import javax.xml.parsers.ParserConfigurationException;
// import se.kth.castor.jdbl.util.JarUtils;
// import se.kth.castor.jdbl.wrapper.DebloatTypeEnum;
// import se.kth.castor.jdbl.wrapper.JacocoWrapper;
//
// public class MethodDebloaterTest {
//
//    private File baseDir;
//    private String outputDirectory;
//
//    @Before
//    public void setUp() {
//
//
//       this.baseDir = new File("dummy-project");
//       this.outputDirectory = "dummy-project/target/classes";
//    }
//
//    @Test
//    public void pomExists(){
//       File projectBase = new File("dummy-project");
//       File pom = new File(projectBase, "pom.xml");
//       Assert.assertNotNull(pom);
//       Assert.assertTrue(pom.exists());
//    }
//
//    public void removeUnusedMethods() {
//       JacocoWrapper jacocoWrapper = new JacocoWrapper(this.baseDir, new File(this.baseDir.getAbsolutePath() + "/report.xml"), DebloatTypeEnum.TEST_DEBLOAT);
//       Map<String, Set<String>> usageAnalysis = null;
//
//       // run the usage analysis
//       try {
//          usageAnalysis = jacocoWrapper.analyzeUsages();
//       } catch (IOException | ParserConfigurationException | SAXException e) {
//          e.printStackTrace();
//       }
//
//       // decompress the jar files in the output directory
//       JarUtils.decompressJars(this.outputDirectory);
//
//       Set<String> classesUsed = new HashSet<>();
//
//       for (Map.Entry<String, Set<String>> entry : usageAnalysis.entrySet()) {
//          if (entry.getValue() != null) {
//             classesUsed.add(entry.getKey());
//             System.out.println(entry.getKey() + " = " + entry.getValue());
//          }
//       }
//
//       EntryPointMethodDebloat methodDebloater = new EntryPointMethodDebloat(this.outputDirectory, usageAnalysis);
//       try {
//          methodDebloater.removeUnusedMethods();
//       } catch (IOException e) {
//          e.printStackTrace();
//       }
//
//       // print some results
//       System.out.println("#unused classes: " +
//          usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count());
//       System.out.println("#unused methods: " +
//          usageAnalysis.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getValue()).mapToInt(s -> s.size()).sum());
//    }
// }
