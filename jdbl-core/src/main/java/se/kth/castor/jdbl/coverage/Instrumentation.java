package se.kth.castor.jdbl.coverage;

import java.io.File;

/**
 * Information for a class instrumentation operation.
 */
class Instrumentation {
    /**
     * The directory of classes to be instrumented.
     */
    private File baseDirectory;

    /**
     * The directory into which instrumented classes should be placed.
     */
    private File instrDirectory;

    /**
     * Initialize an {@code Instrumentation} with basic data.
     *
     * @param baseDirectory The directory of classes to be instrumented.
     * @param instrDirectory The directory into which instrumented
     *                       classes should be placed.
     */
    Instrumentation(final File baseDirectory,
        final File instrDirectory) {
        this.baseDirectory = baseDirectory;
        this.instrDirectory = instrDirectory;
    }

    /**
     * Get the directory of classes to be instrumented.
     *
     * @return The directory of classes to be instrumented.
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Get the directory into which instrumented classes should be placed.
     *
     * @return The directory into which instrumented classes should be placed.
     */
    public File getInstrDirectory() {
        return instrDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "baseDir: " + baseDirectory.getPath() +
            ", instrDir: " + instrDirectory.getPath();
    }
}

