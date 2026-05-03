package com.riskmonitor;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Stock Risk Monitor (Java)...");

        List<Position> portfolio = Position.defaultPortfolio();
        for (Position p : portfolio) {
            System.out.printf("  %s: %.0f shares @ $%.2f%n",
                p.getTicker(), p.getQuantity(), p.getBuyPrice());
        }
    }
}
