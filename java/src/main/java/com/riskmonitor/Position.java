package com.riskmonitor;

/**
 * Represents a single portfolio holding.
 *
 * In Go this would be a struct with no methods - plain data.
 * In Java even a simple data holder becomes a class.
 * Java 16+ records reduce this boilerplate significantly,
 * but most enterprise codebases still use plain classes.
 */
public class Position {
    private final String ticker;
    private final double quantity;
    private final double buyPrice;
    private final double alertDown; // alert threshold in %

    public Position(String ticker, double quantity, double buyPrice, double alertDown) {
        this.ticker = ticker;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.alertDown = alertDown;
    }

    public String getTicker()    { return ticker; }
    public double getQuantity()  { return quantity; }
    public double getBuyPrice()  { return buyPrice; }
    public double getAlertDown() { return alertDown; }
}
