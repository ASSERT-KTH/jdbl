package se.kth.jdbl.loader;

import java.util.Iterator;
import java.util.Vector;

public class LoaderCollector {

    public static Iterator list(ClassLoader classLoader) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class CL_class = classLoader.getClass();
        while (CL_class != java.lang.ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        java.lang.reflect.Field ClassLoader_classes_field = CL_class
                .getDeclaredField("classes");
        ClassLoader_classes_field.setAccessible(true);
        Vector classes = (Vector) ClassLoader_classes_field.get(classLoader);
        return classes.iterator();
    }
}
