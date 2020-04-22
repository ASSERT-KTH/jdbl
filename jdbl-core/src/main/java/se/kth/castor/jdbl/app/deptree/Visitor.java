package se.kth.castor.jdbl.app.deptree;

public interface Visitor
{
   void visit(Node tree) throws VisitException;
}
