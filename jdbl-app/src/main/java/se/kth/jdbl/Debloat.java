package se.kth.jdbl;

import java.io.File;
import java.io.IOException;

public interface Debloat {

    /**
     * Debloat a jar file via static and dynamic analysis of execution traces.
     *
     * @param input The jar file to be debloated.
     * @return A debloated jar file.
     */
    File debloat(File input) throws IOException;
}
