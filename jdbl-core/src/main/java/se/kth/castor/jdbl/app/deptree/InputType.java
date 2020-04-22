package se.kth.castor.jdbl.app.deptree;

public enum InputType
{
   TEXT {
      @Override
      public Parser newParser()
      {
         return new TextParser();
      }
   };

   public abstract Parser newParser();
}
