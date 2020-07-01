package se.kth.castor.jdbl.deptree;

public interface Visitor
{
    void visit(Node tree) throws VisitException;
}
