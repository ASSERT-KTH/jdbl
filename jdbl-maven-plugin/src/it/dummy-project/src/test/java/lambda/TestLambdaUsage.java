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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import common.Order;
import common.Order.Market;
import common.Order.Type;
import static org.junit.Assert.*;

/**
 * Test class for demonstrating use of lambdas in Java 8.
 * <p>
 * Use case is for querying orders in a trading exchange order book.
 * <p>
 *
 * @author gazbert
 */
public class TestLambdaUsage
{

    private List<Order> orderBook;

    /**
     * Builds the order book up for each test.
     */
    @Before
    public void setupForEachTest()
    {
        final Order order1 = new Order(
            Market.EUR, Type.BUY, new BigDecimal("100.00"), new BigDecimal("1.69"), new BigDecimal("0.01"));
        final Order order2 = new Order(
            Market.USD, Type.BUY, new BigDecimal("200.00"), new BigDecimal("1.70"), new BigDecimal("0.01"));
        final Order order3 = new Order(
            Market.CNY, Type.SELL, new BigDecimal("250.00"), new BigDecimal("10.58"), new BigDecimal("0.01"));

        orderBook = new ArrayList<>();
        orderBook.add(order1);
        orderBook.add(order2);
        orderBook.add(order3);
    }

    /**
     * Shows the Java 7 way of doing things.
     * <p>
     * We create a bunch of orders. We then check the orders and count only the ones that are over a certain price.
     * <p>
     */
    @Test
    public void countMatchingOrdersUsingJava7WayOfUsingAnonymousClass()
    {

        /*
         * Check for number of orders with prices of 1.70 or more
         */
        final int orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(

            orderBook,

            // typically use anonymous inner class with 1 method.
            new OrderMatcher()
            {
                @Override
                public boolean executeQuery(Order order)
                {
                    if (order.getPrice().compareTo(new BigDecimal(1.70)) >= 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

        assertEquals(2, orderCount);
    }

    /**
     * Shows the Java 8 way of doing things.
     * <p>
     * Ok, hopefully the first test looked familiar. Note we used an anonmous inner class with a single method.
     * This is called a functional interface; a functional interface is any interface that contains only one abstract
     * method.
     * <p>
     * It's quite cumbersome looking and a pain to type. This is where lambdas come in.. :-)
     */
    @Test
    public void countMatchingOrdersUsingJava8LambdaExpression()
    {

        /*
         * Check for number of orders with prices of 1.70 or more
         */
        int orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(

            // pass the order book like we did before
            orderBook,

            /*
             * But now we use a lambda expression - much cleaner!
             *
             * The (Order order) part is the argument to the function.
             * The -> arrow token part 'points' to the function.
             * Everything the other side of the arrow is the expression.
             */
            (Order o) -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0);

        assertEquals(2, orderCount);

        /*
         * We can omit the data type of the parameters in a lambda expression to write less code...
         */
        orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook, (o) -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0);

        assertEquals(2, orderCount);

        /*
         * In addition, we can omit the parentheses if there is only one parameter.
         */
        orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook, o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0);

        assertEquals(2, orderCount);

        /*
         * If we wanted to have multiple expressions in our lambda function, we need to include {} and a return
         * statement.
         *
         * In this case we want to make sure the order was a SELL order.
         */
        orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook,
            o -> {
                if (o.getPrice().compareTo(new BigDecimal("1.70")) >= 0) {
                    if (o.getType() == Type.SELL) {
                        return true;
                    }
                }
                return false;
            }
        );

        // only 1 order matches now
        assertEquals(1, orderCount);

        /*
         * I could have coded the last example far simpler, but I wanted to show the use of blocks within lambdas.
         *
         * Below is how I would code it normally.
         */
        orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook, o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0 && o.getType() == Type.SELL);

        // only 1 order matches now
        assertEquals(1, orderCount);
    }

    /**
     * In this example we show syntax for passing 2 args to a lambda function.
     * <p>
     * The use case is to check for orders with prices over 1.70
     * Without the fee, only 2 orders match; with the fee, 3 orders match.
     */
    @Test
    public void showJava8WayOfCountingMatchedOrdersUsing2ArgsToLambdaFunction()
    {

        /*
         * No fee; 2 orders match
         */
        int orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook,
            o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0);
        assertEquals(2, orderCount);

        /*
         * Addition of the fee means we have 3 orders that match.
         * Note 2 args passed to lambda function.
         */
        orderCount = OrderBookAnalyser.getNumberOfMatchedOrders(
            orderBook,
            (Order o, BigDecimal fee) -> o.getPrice().add(o.getFee()).setScale(2, RoundingMode.HALF_UP).compareTo(
                new BigDecimal("1.70")) >= 0);

        assertEquals(3, orderCount);
    }

    /**
     * Shows the Java 8 way of doing things using the JDK Standard Functional Interfaces.
     * <p>
     * Given we only have 1 method with 1 arg in our {@link OrderMatcher} interface, we could use the
     * inbuilt JDK functional interfaces defined in the <code>java.se.kth.castor.jdbl.util.util.function<code> package.
     * <p>
     * More detail on standard functions here:
     * http://download.java.net/lambda/b81/docs/api/java/util/function/package-summary.html
     * <p>
     * Here's how we can use the {@link Predicate} functional interface for our use case.
     */
    @Test
    public void showJava8WayOfCountingMatchedOrdersUsingJdkPredicateFunctionalInterface()
    {

        /*
         * Check for number of orders with prices of 1.70 or more
         * Everything is exactly the as before in the test code; take a look at the new method we are calling though...
         */
        int orderCount = OrderBookAnalyser.getNumberOfMatchedOrdersUsingJdkPredicateFunctionalInterface(
            orderBook,
            o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0);

        assertEquals(2, orderCount);
    }

    /**
     * Shows another example of doing things using the JDK Standard Functional Interfaces.
     * <p>
     * More detail on standard functions here:
     * http://download.java.net/lambda/b81/docs/api/java/util/function/package-summary.html
     * <p>
     * Use case: all order amounts over 200 need to be audited.
     */
    @Test
    public void showGettingAuditDetailsUsingJava8JdkFunctionFunctionalInterface()
    {

        List<String> auditDetails = OrderBookAnalyser.getAuditDetailsForMatchingOrders(
            orderBook,
            o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0,
            o -> o.provideAuditDetails());

        assertEquals(2, auditDetails.size());
        assertTrue(auditDetails.get(0).contains("USD")); // eek! But you get the idea ;-)
        assertTrue(auditDetails.get(1).contains("CNY"));
    }

    /**
     * Use case as previous example.
     * <p>
     * This time, we're going to make things more Generic when we use the JDK Standard Functional Interfaces.
     * <p>
     * More detail on standard functions here:
     * http://download.java.net/lambda/b81/docs/api/java/util/function/package-summary.html
     * <p>
     * Use case: all order amounts over 200 need to be audited.
     */
    @Test
    public void showGettingAuditDetailsUsingGenericArgsAndJava8JdkFunctionalInterfaces()
    {

        List<String> auditDetails = OrderBookAnalyser.getAuditDetailsForMatchingOrdersUsingGenericArgs(
            orderBook,
            o -> o.getPrice().compareTo(new BigDecimal("1.70")) >= 0,
            o -> o.provideAuditDetails());

        assertEquals(2, auditDetails.size());
        assertTrue(auditDetails.get(0).contains("USD")); // eek! But you get the idea ;-)
        assertTrue(auditDetails.get(1).contains("CNY"));
    }
}
