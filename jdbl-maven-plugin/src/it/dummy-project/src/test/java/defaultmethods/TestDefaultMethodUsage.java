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

package defaultmethods;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import common.Order;

/**
 * Test class for demonstrating use of default and static methods on interfaces in Java 8.
 * <p>
 *
 * @author gazbert
 *
 */
public class TestDefaultMethodUsage
{
    /**
     * Shows use of default method.
     * <p>
     * When you extend an interface that contains a default method, you treat it just like a superclass method:
     * <ul>
     * <li>Not mention the default method at all - lets your extended interface inherit the default method.</li>
     * <li>Redeclare the default method - makes it abstract.</li>
     * <li>Redefine the default method - which overrides it.</li>
     * </ul>
     */
    @Test
    public void showDefaultMethodUsage()
    {
        final TradingApi api = new MyTradingApi();
        final List<Order> openOrders = api.getOpenOrders(1);
        assertEquals(0, openOrders.size());

        // now invoke the default method
        assertEquals("Default API Impl", api.getImplName());
    }

    /**
     * Shows how to use static methods on Java 8 interfaces.
     * <p>
     * Note the use of Date - another demo will cover the new Java 8 java.time API :-)
     */
    @Test
    public void showStaticInterfaceMethodUsage()
    {
        final Date time = TradingApi.getCurrentExchangeTime();
        System.out.println("Exchange clock time: " + time);
    }
}
