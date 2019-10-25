package se.kth.jdbl;

import java.io.File;
import java.util.List;

public class DebloatBuilder {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private File inputFilePath;
    private File outputFilePath;
    private String entryClass;
    private String entryMethod;
    private List<String> entryParams;

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    public DebloatBuilder addInputFilePath(File inputFilePath) {
        this.inputFilePath = inputFilePath;
        return this;
    }

    public DebloatBuilder addOutputFilePath(File outputFilePath) {
        this.outputFilePath = outputFilePath;
        return this;
    }

    public DebloatBuilder addEntryClass(String entryClass) {
        this.entryClass = entryClass;
        return this;
    }

    public DebloatBuilder addEntryMethod(String entryMethod) {
        this.entryMethod = entryMethod;
        return this;
    }

    public DebloatBuilder addEntryParam(List<String> entryParams) {
        this.entryParams = entryParams;
        return this;
    }

    /**
     * Return the finally constructed DebloaterBuilder object.
     */
    public DebloatBuilder build() {
        DebloatBuilder debloatBuilder = new DebloatBuilder();
        debloatBuilder.inputFilePath = this.inputFilePath;
        debloatBuilder.outputFilePath = this.outputFilePath;
        debloatBuilder.entryClass = this.entryClass;
        debloatBuilder.entryMethod = this.entryMethod;
        debloatBuilder.entryParams = this.entryParams;
        return debloatBuilder;
    }

    //--------------------------------/
    //------- GETTER METHOD/S -------/
    //------------------------------/

    public File getInputFilePath() {
        return inputFilePath;
    }

    public File getOutputFilePath() {
        return outputFilePath;
    }

    public String getEntryClass() {
        return entryClass;
    }

    public String getEntryMethod() {
        return entryMethod;
    }

    public List<String> getEntryParams() {
        return entryParams;
    }

    @Override
    public String toString() {
        return "inputFilePath=" + inputFilePath +
                ", outputFilePath=" + outputFilePath +
                ", entryClass='" + entryClass + '\'' +
                ", entryMethod='" + entryMethod + '\'' +
                ", entryParams='" + entryParams;
    }
}
