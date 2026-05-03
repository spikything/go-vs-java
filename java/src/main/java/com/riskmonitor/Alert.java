package com.riskmonitor;

import java.time.Instant;

/**
 * Alert - a fired risk event.
 *
 * In Go this is a plain struct. Java 16+ records give similar brevity:
 *   record Alert(String ticker, String message, ...) {}
 * but we use a class here so it works on Java 11+ too.
 */
public class Alert {
    public final String ticker;
    public final String message;
    public final double price;
    public final double pnlPct;
    public final Instant timestamp;

    public Alert(String ticker, String message, double price, double pnlPct, Instant timestamp) {
        this.ticker = ticker;
        this.message = message;
        this.price = price;
        this.pnlPct = pnlPct;
        this.timestamp = timestamp;
    }
}
