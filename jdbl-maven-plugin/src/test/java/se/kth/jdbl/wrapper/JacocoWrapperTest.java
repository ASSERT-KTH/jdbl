package se.kth.jdbl.wrapper;

public class JacocoWrapperTest {

    /*private JacocoWrapper jacocoWrapper;

    @Before
    public void setUp() {
        File workingDir = new File("/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/dummy-project");
        File report = new File(workingDir, "report.xml");
        jacocoWrapper = new JacocoWrapper(workingDir, report);
    }

    @Test
    public void name() throws IOException, ParserConfigurationException, SAXException {

        Map<String, Set<String>> u = jacocoWrapper.analyzeUsages();

        System.out.println("#unused classes: " + u.entrySet().stream().filter(e -> e.getValue() == null).count());
        System.out.println("#unused methods: " + u.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getValue()).mapToInt(s -> s.size()).sum());

//        Assert.assertEquals(u.entrySet().stream().filter(e -> e.getValue() == null).count(), 606);
//        Assert.assertEquals(u.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getValue()).mapToInt(s -> s.size()).sum(), 9);
    }*/
}