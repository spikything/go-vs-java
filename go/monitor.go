package main

import (
	"fmt"
	"sync"
	"time"
)

// Alert represents a triggered risk alert
type Alert struct {
	Ticker    string
	Message   string
	Price     float64
	PnLPct    float64
	Timestamp time.Time
}

// Monitor is the core engine - it owns all ticker state and the alert log
type Monitor struct {
	mu      sync.RWMutex
	tickers map[string]*TickerState
	alerts  []Alert
}

// NewMonitor initialises a Monitor from a portfolio
func NewMonitor(positions []Position) *Monitor {
	m := &Monitor{
		tickers: make(map[string]*TickerState),
	}
	for _, p := range positions {
		m.tickers[p.Ticker] = &TickerState{
			Position:     p,
			CurrentPrice: p.BuyPrice, // start at buy price
		}
	}
	return m
}

// Run starts one goroutine per ticker and one for the summary ticker.
// All goroutines are shut down cleanly when ctx is cancelled.
//
// This is the key Go pattern: goroutines are cheap (~4KB stack vs ~1MB Java thread),
// so we spin one up per ticker without thinking twice.
func (m *Monitor) Run(done <-chan struct{}, alertCh chan<- Alert) {
	var wg sync.WaitGroup

	for ticker := range m.tickers {
		wg.Add(1)
		go func(t string) {
			defer wg.Done()
			m.monitorTicker(t, done, alertCh)
		}(ticker)
	}

	wg.Wait()
}

// monitorTicker runs the price simulation loop for a single ticker.
// It uses a time.Ticker (Go's scheduled channel) instead of sleep loops,
// which is more accurate and idiomatic.
func (m *Monitor) monitorTicker(ticker string, done <-chan struct{}, alertCh chan<- Alert) {
	// update price every 500ms - fast enough to feel live
	priceTick := time.NewTicker(500 * time.Millisecond)
	defer priceTick.Stop()

	// volatility per ticker - TSLA is noisier than JPM
	volatilities := map[string]float64{
		"AAPL": 0.008,
		"MSFT": 0.007,
		"NVDA": 0.015,
		"JPM":  0.005,
		"TSLA": 0.020,
	}
	vol := volatilities[ticker]

	for {
		// select blocks until one of these cases is ready -
		// this is Go's core concurrency primitive, cleaner than
		// Java's blocking queues or future chaining
		select {
		case <-done:
			return

		case <-priceTick.C:
			ts := m.tickers[ticker]
			ts.mu.Lock()

			newPrice := simulatePrice(ts.CurrentPrice, vol)
			ts.CurrentPrice = newPrice

			// maintain rolling 60-point history (~30 seconds)
			ts.PriceHistory = append(ts.PriceHistory, PricePoint{
				Price:     newPrice,
				Timestamp: time.Now(),
			})
			if len(ts.PriceHistory) > 60 {
				ts.PriceHistory = ts.PriceHistory[1:]
			}

			// check drawdown alert
			pnlPct := ((newPrice - ts.Position.BuyPrice) / ts.Position.BuyPrice) * 100
			if pnlPct < -ts.Position.AlertDown && !ts.AlertFired {
				ts.AlertFired = true
				alert := Alert{
					Ticker:    ticker,
					Message:   fmt.Sprintf("DRAWDOWN ALERT: %.2f%% below buy price", -pnlPct),
					Price:     newPrice,
					PnLPct:    pnlPct,
					Timestamp: time.Now(),
				}
				ts.mu.Unlock()

				// non-blocking send - if the UI is busy we skip rather than block
				select {
				case alertCh <- alert:
				default:
				}

				// log it
				m.mu.Lock()
				m.alerts = append(m.alerts, alert)
				if len(m.alerts) > 20 {
					m.alerts = m.alerts[1:]
				}
				m.mu.Unlock()
				continue
			}

			// reset alert if price recovers
			if pnlPct > -ts.Position.AlertDown {
				ts.AlertFired = false
			}

			ts.mu.Unlock()
		}
	}
}

// Snapshot returns a consistent read of all ticker states for the UI
func (m *Monitor) Snapshot() map[string]TickerSnapshot {
	snap := make(map[string]TickerSnapshot)
	for ticker, ts := range m.tickers {
		ts.mu.RLock()
		snap[ticker] = TickerSnapshot{
			Ticker:       ticker,
			CurrentPrice: ts.CurrentPrice,
			BuyPrice:     ts.Position.BuyPrice,
			Quantity:     ts.Position.Quantity,
			PnL:          (ts.CurrentPrice - ts.Position.BuyPrice) * ts.Position.Quantity,
			PnLPct:       ((ts.CurrentPrice - ts.Position.BuyPrice) / ts.Position.BuyPrice) * 100,
			MarketValue:  ts.CurrentPrice * ts.Position.Quantity,
			Volatility:   ts.Volatility(),
		}
		ts.mu.RUnlock()
	}
	return snap
}

// RecentAlerts returns up to n recent alerts
func (m *Monitor) RecentAlerts(n int) []Alert {
	m.mu.RLock()
	defer m.mu.RUnlock()
	if len(m.alerts) <= n {
		return m.alerts
	}
	return m.alerts[len(m.alerts)-n:]
}

// TickerSnapshot is a point-in-time read of a ticker for display
type TickerSnapshot struct {
	Ticker       string
	CurrentPrice float64
	BuyPrice     float64
	Quantity     float64
	PnL          float64
	PnLPct       float64
	MarketValue  float64
	Volatility   float64
}
