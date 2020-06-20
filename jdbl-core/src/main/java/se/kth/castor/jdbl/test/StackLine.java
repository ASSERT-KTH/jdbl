package se.kth.castor.jdbl.test;

import java.util.Objects;

public class StackLine {
    private String className;
    private String method;
    private String file;
    private int line;

    public StackLine(String className, String method, String file, int line) {
        this.className = className;
        this.method = method;
        this.file = file;
        this.line = line;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackLine stackLine = (StackLine) o;
        return line == stackLine.line &&
                Objects.equals(className, stackLine.className) &&
                Objects.equals(method, stackLine.method) &&
                Objects.equals(file, stackLine.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, method, file, line);
    }

    @Override
    public String toString() {
        return "StackLine{" +
                "className='" + className + '\'' +
                ", method='" + method + '\'' +
                ", file='" + file + '\'' +
                ", line=" + line +
                '}';
    }
}
