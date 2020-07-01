public class Main
{
    public static void main(String[] args)
    {

        Calc calc = new Calc(2,5);
        int result = calc.sum();
        System.out.println("The sum is" + result);
    }

    private static int anotherSum(final int i, final int i1)
    {
        return i + i1;
    }
}
