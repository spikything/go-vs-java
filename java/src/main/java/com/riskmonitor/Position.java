package com.riskmonitor;

import java.util.List;

public class Position {
    private final String ticker;
    private final double quantity;
    private final double buyPrice;
    private final double alertDown;

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

    public static List<Position> defaultPortfolio() {
        return List.of(
            new Position("AAPL", 50,  178.50, 3.0),
            new Position("MSFT", 30,  415.20, 2.5),
            new Position("NVDA", 20,  875.00, 4.0),
            new Position("JPM",  40,  198.75, 2.0),
            new Position("TSLA", 25,  242.00, 5.0)
        );
    }
}
