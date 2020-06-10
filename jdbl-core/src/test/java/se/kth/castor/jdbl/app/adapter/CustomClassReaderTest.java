package se.kth.castor.jdbl.app.adapter;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomClassReaderTest
{
    CustomClassReader ccr;

    @Test
    public void testIsInterface()
    {
        ccr = new CustomClassReader("java.lang.Appendable");
        assertTrue(ccr.isInterface());
        ccr = new CustomClassReader("java.lang.Character");
        assertFalse(ccr.isInterface());
    }

    @Test
    public void testIsException()
    {
        ccr = new CustomClassReader("java.io.FileNotFoundException");
        assertTrue(ccr.isException());
        ccr = new CustomClassReader("java.io.Bits");
        assertFalse(ccr.isException());
    }
}
