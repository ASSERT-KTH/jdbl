package calc.other;

import calc.CalculatorB;
import calc.StaticCl;

public class CalculatorNew extends CalculatorB
{

    int a;
    int b;

    public CalculatorNew(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int sum() {
        return a + b;
    }

    @Override
    public int sub() {
        return a - b;
    }

    @Override
    public int mul() {
        return a * b;
    }

    public int other() {
        return StaticCl.A;
    }

}
