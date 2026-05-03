package com.riskmonitor;

public class TickerSnapshot {
    public final String ticker;
    public final double currentPrice;
    public final double buyPrice;
    public final double quantity;
    public final double pnl;
    public final double pnlPct;
    public final double marketValue;
    public final double volatility;

    public TickerSnapshot(String ticker, double currentPrice, double buyPrice,
                          double quantity, double pnl, double pnlPct,
                          double marketValue, double volatility) {
        this.ticker = ticker;
        this.currentPrice = currentPrice;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.pnl = pnl;
        this.pnlPct = pnlPct;
        this.marketValue = marketValue;
        this.volatility = volatility;
    }
}
