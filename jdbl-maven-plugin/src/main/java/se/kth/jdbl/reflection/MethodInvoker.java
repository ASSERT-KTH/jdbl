package se.kth.jdbl.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoker {

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    /**
     * Invoke method with parameters in a class with a custom {@link ClassLoader}.
     */
    public static void invokeMethod(ClassLoader cl, String entryClass, String entryMethod, String entryParameters) {

        try {
            System.out.println("Entry class: " + entryClass);
            Class entryClassLoaded = cl.loadClass(entryClass);
            System.out.println("Entry class loaded: " + entryClassLoaded.getName());
            Method entryMethodLoaded = entryClassLoaded.getDeclaredMethod(entryMethod, String[].class);
            System.out.println("Entry method loaded: " + entryMethodLoaded.getName());

            if (entryParameters != null) {
                System.out.println("Entry param: " + entryParameters);
                String[] parameters = entryParameters.split(" ");
                System.out.println("Entry param 0: " + parameters[0]);
                entryMethodLoaded.invoke(null, (Object) parameters);
            } else {
                entryMethodLoaded.invoke(null, new Object[]{});
            }
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            System.err.println(e.toString());
        }
    }
}
