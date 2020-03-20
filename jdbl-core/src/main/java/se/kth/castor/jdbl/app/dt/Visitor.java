package se.kth.castor.jdbl.app.dt;

public interface Visitor
{
   void visit(Node tree) throws VisitException;
}
