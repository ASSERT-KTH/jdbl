package se.kth.castor.jdbl.app.dt;

import java.io.Reader;

public interface Parser
{

   Node parse(Reader reader) throws ParseException;

}
