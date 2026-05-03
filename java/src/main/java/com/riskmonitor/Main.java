package com.riskmonitor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point - wires everything together.
 *
 * Compare with Go's main.go:
 * - Go uses channels for shutdown signalling; Java uses a shutdown hook
 * - Go's goroutine model makes the concurrency intent more visible
 * - Java's verbose type declarations vs Go's := inference
 * - Both are ~40 lines - similar here, bigger gap in Monitor
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Stock Risk Monitor (Java)...");

        List<Position> portfolio = Monitor.defaultPortfolio();
        Monitor monitor = new Monitor(portfolio);
        UI ui = new UI();

        // register shutdown hook for Ctrl+C - Java's equivalent of Go's signal.Notify
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("\n\nShutting down...");
            monitor.stop();
            ui.close();
            System.out.println(" done.");
        }));

        // start the price simulation engine
        monitor.start();

        // UI refresh loop - Java needs an explicit scheduler, Go uses a goroutine + time.Ticker
        ScheduledExecutorService uiScheduler = Executors.newSingleThreadScheduledExecutor();
        uiScheduler.scheduleAtFixedRate(() -> {
            Map<String, TickerSnapshot> snap = monitor.snapshot();
            List<Alert> alerts = monitor.recentAlerts(10);
            ui.render(snap, alerts);
        }, 500, 500, TimeUnit.MILLISECONDS);

        // block the main thread - in Go this would be <-sigCh (channel receive)
        Thread.currentThread().join();
    }
}
