package com.riskmonitor;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Holds live state for a single ticker.
 *
 * Compare to Go's TickerState struct - the concept is identical but Java
 * requires explicit class declaration, getter methods, and more ceremony
 * around the ReadWriteLock vs Go's sync.RWMutex which integrates naturally
 * with the defer pattern.
 */
public class TickerState {

    // Java requires explicit lock objects; Go embeds sync.RWMutex in the struct
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Position position;
    private double currentPrice;
    private final Deque<Double> priceHistory = new ArrayDeque<>(60);
    private boolean alertFired = false;

    public TickerState(Position position) {
        this.position = position;
        this.currentPrice = position.getBuyPrice();
    }

    public double getPnL() {
        lock.readLock().lock();
        try {
            return (currentPrice - position.getBuyPrice()) * position.getQuantity();
        } finally {
            lock.readLock().unlock(); // Java has no defer - must use try/finally
        }
    }

    public double getPnLPct() {
        lock.readLock().lock();
        try {
            return ((currentPrice - position.getBuyPrice()) / position.getBuyPrice()) * 100;
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getMarketValue() {
        lock.readLock().lock();
        try {
            return currentPrice * position.getQuantity();
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getVolatility() {
        lock.readLock().lock();
        try {
            if (priceHistory.size() < 2) return 0;
            double[] prices = priceHistory.stream().mapToDouble(Double::doubleValue).toArray();
            double mean = 0;
            for (double p : prices) mean += p;
            mean /= prices.length;
            double variance = 0;
            for (double p : prices) variance += (p - mean) * (p - mean);
            return Math.sqrt(variance / prices.length);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Write operations - called from monitor thread only
    public void writeLock()   { lock.writeLock().lock(); }
    public void writeUnlock() { lock.writeLock().unlock(); }

    public void setCurrentPrice(double price) { this.currentPrice = price; }

    public void addPriceHistory(double price) {
        priceHistory.addLast(price);
        if (priceHistory.size() > 60) priceHistory.pollFirst();
    }

    public boolean isAlertFired() { return alertFired; }
    public void setAlertFired(boolean fired) { this.alertFired = fired; }

    public Position getPosition()    { return position; }
    public double getCurrentPrice()  { return currentPrice; }
}
