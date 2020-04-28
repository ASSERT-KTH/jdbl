/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package lambda;

import java.util.function.Consumer;

/**
 * Demonstrates lambda scoping. Based on the example in the Java 8 tutorial.
 * <p>
 * Lambdas <em>capture</em> variables like local and anonymous classes. In other words, they have the same access to
 * local variables of the enclosing scope.
 * <p>
 * One key difference though, is that lambdas do not suffer from <em>shadowing</em> issues, e.g. where a local class
 * has the same variable and that is used in place of/hides the variable in the outer scope.
 * <p>
 * Lambda expressions are lexically scoped: they do not inherit any names from a supertype or introduce a new level of
 * scoping.
 * <p>
 * See the example below.
 *
 * @author gazbert
 */
public class LambdaScoping
{

    /**
     * Outermost level scoped x arg
     */
    public int x = 1; // 3 // change this to 3 and LambdaScoping.this.x sysout will print 3

    /**
     * First level nested class.
     *
     * @author gazbert
     */
    class FirstLevel
    {
        /**
         * First level scoped x arg
         */
        public int x = 2; // 5 // change this to 5 and this.x sysout will print 5

        /**
         * x arg is passed in by the caller.
         *
         * @param x
         */
        void methodInFirstLevel(int x)
        {
            /*
             * The following statement causes the compiler to generate
             * the error "Local variable x defined in an enclosing scope must be final or effectively final"
             *
             * Like local and anonymous classes, a lambda expression can only access local variables and parameters of
             * the enclosing block that are final or effectively final.
             */
            //x = 99;

            /*
             * Our lambda uses JDK standard Consumer functional interface.
             * More details on standard functions here:
             * http://download.java.net/lambda/b81/docs/api/java/util/function/package-summary.html
             *
             * If you change y arg to be x arg, compiler will error "Lambda expression's parameter x cannot redeclare
             * another local variable defined in an enclosing scope."
             */
            Consumer<Integer> myConsumer = (y) -> {

                // x is the arg value passed into the first level methodInFirstLevel().
                System.out.println("x = " + x); // prints 10

                // y is the value passed into the accept() method called by the methodInFirstLevel() method.
                System.out.println("y = " + y); // prints 10

                // Lambda can access the x instance variable in first level nested class using 'this'
                System.out.println("this.x = " + this.x); // prints 2

                // Lambda can access the x intance variable in the outermost scope
                System.out.println("LambdaScoping.this.x = " + LambdaScoping.this.x); // prints 1
            };

            // Call the lambda expression. The x arg beomes the the y arg in the lambda
            myConsumer.accept(x); // 15 // set this to 15 and y sysout will print 15
        }
    }

    /**
     * Runs the scoping test.
     *
     * @param args
     */
    public static void main(String... args)
    {
        final LambdaScoping outerClass = new LambdaScoping();
        final LambdaScoping.FirstLevel firstLevelClass = outerClass.new FirstLevel();
        firstLevelClass.methodInFirstLevel(10);
    }
}
