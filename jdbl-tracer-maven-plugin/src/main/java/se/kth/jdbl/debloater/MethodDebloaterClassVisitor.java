package se.kth.jdbl.debloater;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import se.kth.jdbl.instrumenter.UsageRecorder;

public class MethodDebloaterClassVisitor extends ClassVisitor implements Opcodes {

    private String className;
    private int nbMethodsRemoved;

    /**
     * Constructor.
     *
     * @param cv the ClassVisitor to pass.
     */
    public MethodDebloaterClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
        nbMethodsRemoved = 0;
    }

    public int getNbMethodsRemoved() {
        return nbMethodsRemoved;
    }

    /**
     * Visit the header of the class. This method visits only classesUsed (skips Enums and Interfaces).
     */
    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
//        isClass = (access & Opcodes.ACC_INTERFACE) == 0 && (access & Opcodes.ACC_ENUM) == 0;
//        this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
//        this.cName = name;
//        classKey = "__instHit" + cName.replace("/", "_");
    }

    /**
     * Removes uncovered methods.
     *
     */
    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        String thisMethod = className + "." + name + desc;
        if (UsageRecorder.getUsedMethods().contains(thisMethod)) {
            // Do nothing
            return super.visitMethod(access, name, desc, signature, exceptions);
        } else {
            // The method was not visited, so it will be removed from the class by returning null
            nbMethodsRemoved++;
            return null;
        }
    }

}
