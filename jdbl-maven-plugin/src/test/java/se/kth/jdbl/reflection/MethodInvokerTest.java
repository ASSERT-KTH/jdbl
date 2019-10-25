package se.kth.jdbl.reflection;

//public class MethodInvokerTest {
//
//    public void invokeMethod() {
//
//        String entryClass = "bag.BagClass";
//        String entryMethod = "main";
//        String entryParameters = "2 3";
//        String outputDirectory = "/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/dummy-project/target/classes";
//
////        ClassLoader entryPointClassLoader = null;
////        entryPointClassLoader = new EntryPointClassLoader(outputDirectory, EntryPointClassLoader.class.getClassLoader());
////        Thread.currentThread().setContextClassLoader(entryPointClassLoader);
//
////        ClassLoader entryPointClassLoader = null;
////        try {
////            entryPointClassLoader = new EntryPointClassLoader(outputDirectory, EntryPointClassLoader.class.getClassLoader());
////            Thread.currentThread().setContextClassLoader(entryPointClassLoader);
////
////            String className = entryClass;
////            Class entryClassLoaded = entryPointClassLoader.loadClass(className);
////
////            Method entryMethodLoaded = entryClassLoaded.getDeclaredMethod(entryMethod, String[].class);
////
////            if (entryParameters != null) {
////                String[] parameters = entryParameters.split(" ");
////
////                entryMethodLoaded.invoke(null, (Object) parameters);
////            } else {
////                entryMethodLoaded.invoke(null, new Object[]{});
////            }
////        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
////                ClassNotFoundException e) {
////        }
//
////        MethodInvoker.invokeMethod(entryPointClassLoader, entryClass, entryMethod, entryParameters);
//
//        ClassLoader entryPointClassLoader = null;
//        try {
//            entryPointClassLoader = new EntryPointClassLoader(outputDirectory, EntryPointClassLoader.class.getClassLoader());
//            Thread.currentThread().setContextClassLoader(entryPointClassLoader);
//
//            String className = entryClass;
//            Class entryClassLoaded = entryPointClassLoader.loadClass(className);
//
//            Method entryMethodLoaded = entryClassLoaded.getDeclaredMethod(entryMethod, String[].class);
//
//            if (entryParameters != null) {
//                String[] parameters = entryParameters.split(" ");
//
//                // start of logging block
//                System.out.println("Invoking method {" + entryMethodLoaded.getName() + "} in class {" + entryClassLoaded.getName() + "} with parameters {");
//                for (int i = 0; i < parameters.length - 1; i++) {
//                    System.out.println(parameters[i] + ", ");
//                }
//                System.out.println(parameters[parameters.length - 1] + "}\n");
//                // end of logging block
//
//                entryMethodLoaded.invoke(null, (Object) parameters);
//            } else {
//                entryMethodLoaded.invoke(null, new Object[]{});
//            }
//        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
//                ClassNotFoundException e) {
//            System.out.println(e);
//        }
//
//        Set<String> classesLoaded = new HashSet<>();
//
//        while (entryPointClassLoader != null) {
//            System.out.println("ClassLoader: " + entryPointClassLoader);
//            try {
//                for (Iterator iter = LoaderCollector.list(entryPointClassLoader); iter.hasNext(); ) {
//                    String classLoaded = iter.next().toString();
//                    classesLoaded.add(classLoaded.split(" ")[1]);
//
//                    if (entryPointClassLoader.toString().startsWith("se.kth.jdbl.loader")) {
//                        System.out.println("\t" + classLoaded);
//                    }
//
//                }
//            } catch (NoSuchFieldException | IllegalAccessException e) {
//                System.out.println("Error: " + e);
//            }
//            entryPointClassLoader = entryPointClassLoader.getParent();
//        }
//
//        /***************************************************************************/
//
//        JacocoWrapper jacocoWrapper = new JacocoWrapper(new File("/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/dummy-project"),
//                new File("/home/cesarsv/Documents/papers/2019_papers/royal-debloat/jicocowrapper/experiments/dummy-project" + "/report.xml"),
//                DebloatTypeEnum.ENTRY_POINT_DEBLOAT,
//                entryClass,
//                entryMethod,
//                entryParameters,
//                new File("/usr/share/maven"));
//
//        Map<String, Set<String>> usageAnalysis = null;
//
//        // run the usage analysis
//        try {
//            usageAnalysis = jacocoWrapper.analyzeUsages();
//            // print some results
//            System.out.println("#unused classes: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() == null).count());
//            System.out.println("#unused methods: " + usageAnalysis.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getValue()).mapToInt(s -> s.size()).sum());
//        } catch (IOException | ParserConfigurationException | SAXException e) {
//            e.printStackTrace();
//        }
//
//    }
//}