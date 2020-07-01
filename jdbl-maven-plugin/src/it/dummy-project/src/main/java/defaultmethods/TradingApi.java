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


import java.util.Date;
import java.util.List;
import java.util.UUID;

import common.Order;

/**
 * A simple, not real-world, part-built Trading API to show use of Java 8 default methods.
 *
 * @author gazbert
 */
public interface TradingApi {
    /**
     * Adds an order.
     *
     * @param order order to add.
     * @return true if order placed successfully, false otherwise.
     */
    boolean addOrder(Order order);

    /**
     * Cancels an order.
     *
     * @param orderId is of order to cancel.
     * @return true if order cancelled successfully, false otherwise.
     */
    boolean cancelOrder(UUID orderId);

    /**
     * Returns your open orders on the exchange.
     *
     * @param marketId id of market to fetch orders for.
     * @return list of open orders for given market, empty list if none found.
     */
    List<Order> getOpenOrders(int marketId);

    /*
     * Below is the default method.
     * This has been added but the interface is already out there and we don't want to break existing code, so we
     * provide a default impl.
     */

    /**
     * @return the API implementation name.
     */
    default String getImplName() {
        System.out.println(TradingApi.class.getSimpleName() + " getImplName() called");
        return "Default API Impl";
    }

    /*
     * Also new in Java 8 is ability to include staitic methods in the interface.
     *
     * Note the use of Date - another demo will cover the new Java 8 java.time API :-)
     */
    static Date getCurrentExchangeTime() {
        return new Date();
    }
}
