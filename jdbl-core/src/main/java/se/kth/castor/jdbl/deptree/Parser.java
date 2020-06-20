package se.kth.castor.jdbl.deptree;

import java.io.Reader;

public interface Parser
{
   Node parse(Reader reader) throws ParseException;
}
