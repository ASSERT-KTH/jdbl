package se.kth.castor.jdbl.app.dt;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;

public abstract class AbstractTextVisitor implements Visitor
{

   private final StringBuilderWriter sbw;

   private final BufferedWriter bw;

   public AbstractTextVisitor()
   {
      sbw = new StringBuilderWriter();
      bw = new BufferedWriter(sbw);
   }

   public void visit(Node node)
   {
      try {
         writeNode(node);
         for (Node child : node.getChildNodes()) {
            visit(child);
         }
      } catch (IOException ignored) {
      }
   }

   private void writeNode(Node node) throws IOException
   {
      //the tree symbols
      writeTreeSymbols(node);
      //the node itself
      bw.write(node.getArtifactCanonicalForm());
      bw.newLine();
   }

   private void writeTreeSymbols(Node node) throws IOException
   {
      if (node.getParent() != null) {
         writeParentTreeSymbols(node.getParent());
         bw.write(getTreeSymbols(node));
      }
   }

   private void writeParentTreeSymbols(Node node) throws IOException
   {
      if (node.getParent() != null) {
         writeParentTreeSymbols(node.getParent());
         bw.write(getParentTreeSymbols(node));
      }
   }

   public abstract String getTreeSymbols(Node node);

   public abstract String getParentTreeSymbols(Node node);

   @Override
   public String toString()
   {
      try {
         bw.flush();
         sbw.flush();
         return sbw.toString();
      } catch (IOException e) {
         return "";
      } finally {
         try {
            bw.close();
         } catch (IOException ignored) {
         }
         sbw.close();
      }
   }
}
