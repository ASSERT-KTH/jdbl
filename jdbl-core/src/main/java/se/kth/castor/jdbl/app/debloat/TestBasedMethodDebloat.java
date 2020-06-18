package se.kth.castor.jdbl.app.debloat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import se.kth.castor.jdbl.app.coverage.UsageAnalysis;
import se.kth.castor.jdbl.app.test.StackLine;

public class TestBasedMethodDebloat extends AbstractMethodDebloat
{
    protected static final Logger LOGGER = LogManager.getLogger(TestBasedMethodDebloat.class);
    private final Set<StackLine> failingMethods;

    public TestBasedMethodDebloat(String outputDirectory, UsageAnalysis usageAnalysis,
        File reportFile, Set<StackLine> failingMethods)
    {
        super(outputDirectory, usageAnalysis, reportFile);
        this.failingMethods = failingMethods;
    }

    @Override
    public void removeMethod(String clazz, Set<String> usedMethods) throws IOException
    {
        FileInputStream in = new FileInputStream(new File(outputDirectory + "/" + clazz + ".class"));
        ClassReader cr = new ClassReader(in);
        processStackTrace(clazz, usedMethods, cr);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM8, cw)
        {

            boolean isEnum = false;

            @Override
            public void visit(final int version, final int access, final String name, final String signature,
                final String superName, final String[] interfaces)
            {
                super.visit(version, access, name, signature, superName, interfaces);
                if ((access & Opcodes.ACC_ENUM) != 0) {
                    isEnum = true;
                }
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                String nameDesc = name + desc;

                final String valuesMethod = "values()[L" + clazz + ";";
                final String valuesOfMethod = "valueOf(Ljava/lang/String;)L" + clazz + ";";

                if (!usedMethods.contains(name + desc) && !(isDefaultConstructor(nameDesc)) && !isEnum) {
                    LOGGER.info("Removed method: " + nameDesc + " in " + clazz);
                    // Write report to file
                    writeReportToFile(name, desc, "BloatedMethod, ", clazz);
                    return new MethodExceptionThrower(mv); // to completely remove the method, just return null
                } else if (!usedMethods.contains(name + desc) && !(isDefaultConstructor(nameDesc)) &&
                    isEnum &&
                    !nameDesc.equals(valuesMethod) &&
                    !nameDesc.equals(valuesOfMethod)
                ) {
                    LOGGER.info("Removed method: " + nameDesc + " in " + clazz);
                    // Write report to file
                    writeReportToFile(name, desc, "BloatedMethod, ", clazz);
                    return new MethodExceptionThrower(mv); // to completely remove the method, just return null
                } else {
                    LOGGER.info("Retained method: " + nameDesc + " in " + clazz);
                    writeReportToFile(name, desc, "UsedMethod, ", clazz);

                }
                return mv;
            }

        };
        cr.accept(cv, ClassReader.SKIP_DEBUG);
        byte[] code = cw.toByteArray();
        try (OutputStream fos = new FileOutputStream(outputDirectory + "/" + clazz.replace(".", "/") + ".class")) {
            fos.write(code);
        } catch (Exception ignored) {
            LOGGER.error("Error replacing class " + clazz + " with debloated methods ");
        }
    }

    private void processStackTrace(final String clazz, final Set<String> usedMethods, final ClassReader cr)
    {
        Set<StackLine> methods = getStackLines(clazz);
        ClassNode clNode = new ClassNode(Opcodes.ASM8);
        cr.accept(clNode, Opcodes.ASM8);
        for (MethodNode mNode : clNode.methods) {
            // ignoreOneLineMethods(usedMethods, mNode);
            for (StackLine failingMethod : methods) {
                if (mNode.name.equals(failingMethod.getMethod())) {
                    for (AbstractInsnNode inNode : mNode.instructions) {
                        addStacktraceMethod(usedMethods, mNode, failingMethod, inNode);
                    }
                }
            }
        }
    }

    private void addStacktraceMethod(final Set<String> usedMethods, final MethodNode mNode, final StackLine failingMethod, final AbstractInsnNode inNode)
    {
        if (inNode instanceof LineNumberNode && ((LineNumberNode) inNode).line == failingMethod.getLine()) {
            LOGGER.info("Method in stacktrace: " + mNode.name + mNode.desc);
            usedMethods.add(mNode.name + mNode.desc);
        }
    }

    private Set<StackLine> getStackLines(final String clazz)
    {
        Set<StackLine> methods = new HashSet<>();
        for (StackLine failingMethod : failingMethods) {
            if (failingMethod.getClassName().equals(clazz.replace("/", "."))) {
                methods.add(failingMethod);
            }
        }
        return methods;
    }

    private void ignoreOneLineMethods(final Set<String> usedMethods, final MethodNode mNode)
    {
        ListIterator<AbstractInsnNode> iter = mNode.instructions.iterator();
        int counter = 0;
        while (iter.hasNext()) {
            AbstractInsnNode inNode = iter.next();
            if (inNode instanceof LineNumberNode) {
                counter++;
            }
        }
        if (counter <= 1) {
            usedMethods.add(mNode.name + mNode.desc);
        }
    }

    private boolean isDefaultConstructor(String name)
    {
        return name.startsWith("<init>(") || name.startsWith("<clinit>(");
    }

    private void writeReportToFile(final String name, final String desc, final String usageType, String clazz)
    {
        try {
            FileUtils.writeStringToFile(reportFile, usageType + clazz + ":" + name + desc + "\n",
                StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            LOGGER.error("Error writing the methods report.");
        }
    }
}
