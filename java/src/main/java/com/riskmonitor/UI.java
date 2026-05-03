package com.riskmonitor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Renders the live terminal dashboard.
 * Identical visual output to the Go version - same ANSI codes, same layout.
 * Compare the two files side by side to see the syntax difference clearly.
 */
public class UI {

    private static final String RESET      = "\033[0m";
    private static final String BOLD       = "\033[1m";
    private static final String DIM        = "\033[2m";
    private static final String RED        = "\033[31m";
    private static final String GREEN      = "\033[32m";
    private static final String CYAN       = "\033[36m";
    private static final String CLEAR_LINE = "\033[2K";
    private static final String HIDE_CURSOR = "\033[?25l";
    private static final String SHOW_CURSOR = "\033[?25h";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Instant startTime = Instant.now();
    private int frameCount = 0;
    private int lastLines = 0;

    public UI() {
        System.out.print(HIDE_CURSOR);
    }

    public void close() {
        System.out.print(SHOW_CURSOR);
        System.out.println();
    }

    public void render(Map<String, TickerSnapshot> snap, List<Alert> alerts) {
        frameCount++;

        // move cursor up to overwrite previous frame
        if (lastLines > 0) {
            System.out.printf("\033[%dA", lastLines);
        }

        StringBuilder sb = new StringBuilder();
        int lines = 0;

        // Java lacks Go's multi-return and := shorthand so we track lines manually
        lines += writeln(sb, String.format(
            "%s%s  PORTFOLIO RISK MONITOR%s  %s(Java)%s  uptime: %s  frame: %d",
            BOLD, CYAN, RESET, DIM, RESET,
            formatUptime(), frameCount));
        lines += writeln(sb, "─".repeat(72));

        lines += writeln(sb, String.format("%s%-6s  %8s  %8s  %10s  %8s  %8s  %6s%s",
            BOLD, "TICKER", "PRICE", "BUY", "MKT VALUE", "P&L $", "P&L %", "VOL σ", RESET));
        lines += writeln(sb, "─".repeat(72));

        List<String> tickers = new ArrayList<>(snap.keySet());
        Collections.sort(tickers);

        double totalPnL = 0, totalValue = 0;
        for (String t : tickers) {
            TickerSnapshot s = snap.get(t);
            totalPnL += s.pnl;
            totalValue += s.marketValue;

            String pnlColor = s.pnl >= 0 ? GREEN : RED;
            String sign = s.pnl >= 0 ? "+" : "";
            String spark = s.pnlPct >= 0 ? "▲" : "▼";

            lines += writeln(sb, String.format(
                "%s%-6s%s  %8.2f  %8.2f  %10.2f  %s%s%.2f%s  %s%s %.2f%%%s  %6.3f",
                BOLD, t, RESET,
                s.currentPrice, s.buyPrice, s.marketValue,
                pnlColor, sign, s.pnl, RESET,
                pnlColor, spark, s.pnlPct, RESET,
                s.volatility));
        }

        lines += writeln(sb, "─".repeat(72));
        String pnlColor = totalPnL >= 0 ? GREEN : RED;
        String sign = totalPnL >= 0 ? "+" : "";
        lines += writeln(sb, String.format("%sTOTAL%s  %s%33.2f  %s%s%s%.2f%s",
            BOLD, RESET, DIM, totalValue, RESET, pnlColor + BOLD, sign, totalPnL, RESET));

        lines += writeln(sb, "");
        lines += writeln(sb, BOLD + "RISK EXPOSURE" + RESET);
        for (String t : tickers) {
            TickerSnapshot s = snap.get(t);
            double pct = (s.marketValue / totalValue) * 100;
            int barLen = (int) Math.round(pct / 2);
            String bar = "█".repeat(barLen) + "░".repeat(50 - barLen);
            lines += writeln(sb, String.format("  %-6s %s%s%s %.1f%%", t, CYAN, bar, RESET, pct));
        }

        lines += writeln(sb, "");
        lines += writeln(sb, BOLD + "RECENT ALERTS" + RESET);
        if (alerts.isEmpty()) {
            lines += writeln(sb, DIM + "  No alerts fired yet." + RESET);
        } else {
            int start = Math.max(0, alerts.size() - 5);
            for (int i = alerts.size() - 1; i >= start; i--) {
                Alert a = alerts.get(i);
                lines += writeln(sb, String.format("  %s[%s]%s %s%-6s%s  %s",
                    DIM, TIME_FMT.format(a.timestamp), RESET,
                    RED + BOLD, a.ticker, RESET, a.message));
            }
        }

        lines += writeln(sb, "─".repeat(72));
        lines += writeln(sb, DIM + "  Ctrl+C to exit" + RESET);

        System.out.print(sb);
        System.out.flush();
        lastLines = lines;
    }

    private int writeln(StringBuilder sb, String s) {
        sb.append(CLEAR_LINE).append(s).append("\n");
        return 1;
    }

    private String formatUptime() {
        Duration d = Duration.between(startTime, Instant.now());
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        return h > 0
            ? String.format("%dh %dm %ds", h, m, s)
            : String.format("%dm %ds", m, s);
    }
}
