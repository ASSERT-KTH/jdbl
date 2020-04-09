package se.kth.castor.jdbl.app.adapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomClassReaderTest
{
   CustomClassReader ccr;

   @Test
   void testIsInterface()
   {
      ccr = new CustomClassReader("java.lang.Appendable");
      Assertions.assertTrue(ccr.isInterface());

      ccr = new CustomClassReader("java.lang.Character");
      Assertions.assertFalse(ccr.isInterface());
   }
}
