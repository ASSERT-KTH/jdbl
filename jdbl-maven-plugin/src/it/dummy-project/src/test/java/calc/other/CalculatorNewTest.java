package calc.other;

import org.junit.Test;

public class CalculatorNewTest
{

    @Test
    public void sum()
    {
        CalculatorNew calculatorNew = new CalculatorNew(4, 3);
        calculatorNew.sum();
    }
}
