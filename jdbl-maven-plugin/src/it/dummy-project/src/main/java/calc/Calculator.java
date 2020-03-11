package calc;

public class Calculator extends CalculatorB {

    int a;
    int b;

    public Calculator(int a, int b) {
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

}
