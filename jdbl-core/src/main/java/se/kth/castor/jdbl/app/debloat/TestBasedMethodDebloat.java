package se.kth.castor.jdbl.app.debloat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
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

        Set<StackLine> methods = new HashSet<>();
        for (StackLine failingMethod : failingMethods) {
            if (failingMethod.getClassName().equals(clazz.replace("/", "."))) {
                methods.add(failingMethod);
            }
        }
        ClassNode clNode = new ClassNode(Opcodes.ASM8);
        cr.accept(clNode, Opcodes.ASM8);
        for (MethodNode mNode : clNode.methods) {

            // ignoreOneLineMethods(usedMethods, mNode);

            for (StackLine failingMethod : methods) {
                if (mNode.name.equals(failingMethod.getMethod())) {
                    ListIterator<AbstractInsnNode> it = mNode.instructions.iterator();
                    while (it.hasNext()) {
                        AbstractInsnNode inNode = it.next();
                        if (inNode instanceof LineNumberNode) {
                            if (((LineNumberNode) inNode).line == failingMethod.getLine()) {
                                LOGGER.info("Method in stacktrace: " + mNode.name + mNode.desc);
                                usedMethods.add(mNode.name + mNode.desc);
                            }
                        }
                    }
                }
            }
        }

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM8, cw)
        {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                if (!usedMethods.contains(name + desc) && !(isDefaultConstructor(name + desc))) {
                    LOGGER.info("Removed method: " + name + desc + " in " + clazz);
                    // write report to file
                    writeReportToFile(name, desc, "BloatedMethod, ", clazz);
                    return new MethodExceptionThrower(mv);
                    // return null;
                } else {
                    LOGGER.info("Keep method: " + name + desc + " in " + clazz);
                    // write report to file
                    writeReportToFile(name, desc, "UsedMethod, ", clazz);
                }
                return mv;
                // return super.visitMethod(access, name, desc, signature, exceptions);
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
