package calc;

import org.junit.Ignore;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CalculatorTest {

   CalculatorInt calculatorInt;

   @Before
   public void setUp() {
      calculatorInt = new Calculator(2, 3);
   }

   @Test
   public void testSum() {
      assertEquals(5, calculatorInt.sum());
   }

   @Test
   public void testOther() {
      assertEquals(10, ((Calculator) calculatorInt).other());
   }
}
