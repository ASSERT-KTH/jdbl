package se.kth.castor.jdbl.app.dt;

public interface Visitor
{
   public void visit(Node tree) throws VisitException;
}
