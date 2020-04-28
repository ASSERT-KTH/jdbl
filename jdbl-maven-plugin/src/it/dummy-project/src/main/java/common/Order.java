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

package common;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Models an order for placing on an exchange.
 *
 * @author gazbert
 */
public class Order {

    /**
     * Defines the martket ids.
     *
     * @author gazbert
     */
    public enum Market {
        /**
         * Dollar
         */
        USD,

        /**
         * Yuan
         */
        CNY,

        /**
         * Euro
         */
        EUR
    }

    /**
     * Defines the types of order.
     *
     * @author gazbert
     */
    public enum Type {
        /**
         * BUY order
         */
        BUY,

        /**
         * SELL order
         */
        SELL
    }

    /**
     * Id
     */
    private UUID id;

    /**
     * Market id
     */
    private Market marketId;

    /**
     * Type of order BUY|SELL
     */
    private Type type;

    /**
     * Amount of units in the order
     */
    private BigDecimal amount;

    /**
     * Price per unit of order
     */
    private BigDecimal price;

    /**
     * Fee
     */
    private BigDecimal fee;

    /**
     * Number of trades it took to fill the order
     */
    private int tradeCountToFill;


    /**
     * Constructor builds an order.
     *
     * @param marketId the market id.
     * @param type the order type
     * @param amount the order amount.
     * @param price the order price.
     * @param fee the order fee.
     */
    public Order(Market marketId, Type type, BigDecimal amount, BigDecimal price, BigDecimal fee) {

        this.marketId = marketId;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.fee = fee;

        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public Market getMarketId() {
        return marketId;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getFee() {
        return fee;
    }

    /**
     * Returns the order details required for auditing.
     *
     * @return audit details.
     */
    public String provideAuditDetails() {
        // keep it simple for the demo.
        return "OrderId: " + id + " Market: " + marketId + " Amount: " + amount + " Price: " + price;
    }

    public int getTradeCountToFill() {
        return tradeCountToFill;
    }

    public void setTradeCountToFill(int tradeCountToFill) {
        this.tradeCountToFill = tradeCountToFill;
    }
}
