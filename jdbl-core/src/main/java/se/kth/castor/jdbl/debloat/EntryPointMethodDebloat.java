package se.kth.castor.jdbl.debloat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import se.kth.castor.jdbl.coverage.UsageAnalysis;
import se.kth.castor.jdbl.coverage.UsageStatusEnum;
import se.kth.castor.jdbl.util.FileType;

public class EntryPointMethodDebloat extends AbstractMethodDebloat
{
    protected static final Logger LOGGER = LogManager.getLogger(EntryPointMethodDebloat.class);

    public EntryPointMethodDebloat(String outputDirectory, UsageAnalysis usageAnalysis, String projectBaseDir)
    {
        super(outputDirectory, usageAnalysis, projectBaseDir);
    }

    @Override
    public void removeMethod(String clazz, Set<String> usedMethods) throws IOException
    {
        FileInputStream in = new FileInputStream(new File(outputDirectory + "/" + clazz + ".class"));
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM8, cw)
        {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

                if (usedMethods.contains(name + desc)) {
                    System.out.println("Removed unused method: " + name + desc + " in class ==> " + clazz);
                    // write report to file
                    myFileWriter.writeDebloatReport(UsageStatusEnum.BLOATED_METHOD.getName(),
                        clazz + ":" + name + desc, FileType.UNKNOWN);
                    return new MethodExceptionThrower(mv);
                    // return null;
                } else {
                    // write report to file
                    myFileWriter.writeDebloatReport(UsageStatusEnum.USED_METHOD.getName(),
                        clazz + ":" + name + desc, FileType.UNKNOWN);
                }
                return mv;
                // return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };
        cr.accept(cv, ClassReader.SKIP_DEBUG);

        byte[] code = cw.toByteArray();
        try (OutputStream fos = new FileOutputStream(outputDirectory + "/" + clazz.replace(".", "/") + ".class")) {
            fos.write(code);
        }
    }
}
