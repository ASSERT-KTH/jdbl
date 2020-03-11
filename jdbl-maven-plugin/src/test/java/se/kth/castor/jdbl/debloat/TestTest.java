package se.kth.castor.jdbl.debloat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import eu.stamp_project.testrunner.EntryPoint;

public class TestTest {

   @Test
   public void test() {
      try {

         ByteArrayOutputStream outStream = new ByteArrayOutputStream();
         PrintStream outPrint = new PrintStream(outStream);
         EntryPoint.outPrintStream = outPrint;
         EntryPoint.JVMArgs = "-verbose:class";
         EntryPoint.verbose = true;

         EntryPoint.runTests("/home/cesarsv/IdeaProjects/jdbl/jdbl-maven-plugin/src/it/commons-cli/target/classes/:/home/cesarsv/.m2/repository/junit/junit/3.8.1/junit-3.8.1.jar:/home/cesarsv/IdeaProjects/jdbl/jdbl-maven-plugin/src/it/commons-cli/target/classes:/home/cesarsv/IdeaProjects/jdbl/jdbl-maven-plugin/src/it/commons-cli/target/test-classes", "org.apache.commons.cli.ParserTestCase");
         System.out.println(outStream.toString());
         final String[] lines = outStream.toString().split("\n");
         for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            System.out.println(line);
         }
      } catch (TimeoutException e) {
         e.printStackTrace();
      }
   }
}
