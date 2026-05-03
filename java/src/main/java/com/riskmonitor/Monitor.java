package com.riskmonitor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core engine - drives price simulation and alert detection.
 *
 * Go equivalent uses goroutines + channels.
 * Java uses ScheduledExecutorService + BlockingQueue.
 *
 * Both achieve the same thing. The Go version is ~40 lines shorter
 * and the concurrency model is arguably easier to reason about
 * (select{} vs managing thread pools manually).
 */
public class Monitor {

    private final Map<String, TickerState> tickers = new LinkedHashMap<>();
    private final Deque<Alert> alerts = new ArrayDeque<>(20);
    private final Object alertsLock = new Object();

    // Java needs an explicit thread pool; Go goroutines are managed by the runtime
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private final BlockingQueue<Alert> alertQueue = new LinkedBlockingQueue<>(50);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private static final Map<String, Double> VOLATILITIES = Map.of(
        "AAPL", 0.008,
        "MSFT", 0.007,
        "NVDA", 0.015,
        "JPM",  0.005,
        "TSLA", 0.020
    );

    private static final Random random = new Random();

    public Monitor(List<Position> positions) {
        for (Position p : positions) {
            tickers.put(p.getTicker(), new TickerState(p));
        }
    }

    /**
     * Starts one scheduled task per ticker at 500ms intervals.
     *
     * In Go: go monitorTicker(t) - one line per goroutine.
     * In Java: scheduler.scheduleAtFixedRate(...) - requires a Runnable lambda.
     * Functionally equivalent; Go syntax is more direct.
     */
    public void start() {
        for (Map.Entry<String, TickerState> entry : tickers.entrySet()) {
            String ticker = entry.getKey();
            TickerState ts = entry.getValue();
            double vol = VOLATILITIES.getOrDefault(ticker, 0.01);

            scheduler.scheduleAtFixedRate(() -> {
                if (!running.get()) return;
                updateTicker(ticker, ts, vol);
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        running.set(false);
        scheduler.shutdown();
    }

    private void updateTicker(String ticker, TickerState ts, double vol) {
        ts.writeLock();
        try {
            double newPrice = simulatePrice(ts.getCurrentPrice(), vol);
            ts.setCurrentPrice(newPrice);
            ts.addPriceHistory(newPrice);

            double pnlPct = ((newPrice - ts.getPosition().getBuyPrice())
                            / ts.getPosition().getBuyPrice()) * 100;

            if (pnlPct < -ts.getPosition().getAlertDown() && !ts.isAlertFired()) {
                ts.setAlertFired(true);
                Alert alert = new Alert(
                    ticker,
                    String.format("DRAWDOWN ALERT: %.2f%% below buy price", -pnlPct),
                    newPrice,
                    pnlPct,
                    Instant.now()
                );

                // offer() is non-blocking - same intent as Go's select{case alertCh<-:default:}
                alertQueue.offer(alert);

                synchronized (alertsLock) {
                    alerts.addLast(alert);
                    if (alerts.size() > 20) alerts.pollFirst();
                }
            } else if (pnlPct > -ts.getPosition().getAlertDown()) {
                ts.setAlertFired(false);
            }
        } finally {
            ts.writeUnlock(); // try/finally required - Go uses defer
        }
    }

    private static double simulatePrice(double current, double vol) {
        double drift = 0.0001;
        double shock = vol * random.nextGaussian();
        double next = current * Math.exp(drift + shock);
        return Math.max(0.01, next);
    }

    public Map<String, TickerSnapshot> snapshot() {
        Map<String, TickerSnapshot> snap = new LinkedHashMap<>();
        for (Map.Entry<String, TickerState> entry : tickers.entrySet()) {
            String ticker = entry.getKey();
            TickerState ts = entry.getValue();
            snap.put(ticker, new TickerSnapshot(
                ticker,
                ts.getCurrentPrice(),
                ts.getPosition().getBuyPrice(),
                ts.getPosition().getQuantity(),
                ts.getPnL(),
                ts.getPnLPct(),
                ts.getMarketValue(),
                ts.getVolatility()
            ));
        }
        return snap;
    }

    public List<Alert> recentAlerts(int n) {
        synchronized (alertsLock) {
            List<Alert> list = new ArrayList<>(alerts);
            return list.subList(Math.max(0, list.size() - n), list.size());
        }
    }

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
