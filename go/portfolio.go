package main

import (
	"math"
	"math/rand"
	"sync"
	"time"
)

// Position represents a single holding in the portfolio
type Position struct {
	Ticker    string
	Quantity  float64
	BuyPrice  float64
	AlertDown float64 // alert if drawdown % exceeds this
}

// PricePoint is a single price observation with a timestamp
type PricePoint struct {
	Price     float64
	Timestamp time.Time
}

// TickerState holds live state for a single ticker
type TickerState struct {
	mu           sync.RWMutex
	Position     Position
	CurrentPrice float64
	PriceHistory []PricePoint // rolling 30s window
	AlertFired   bool
}

// PnL returns the current profit/loss in dollars
func (ts *TickerState) PnL() float64 {
	ts.mu.RLock()
	defer ts.mu.RUnlock()
	return (ts.CurrentPrice - ts.Position.BuyPrice) * ts.Position.Quantity
}

// PnLPct returns the current P&L as a percentage
func (ts *TickerState) PnLPct() float64 {
	ts.mu.RLock()
	defer ts.mu.RUnlock()
	return ((ts.CurrentPrice - ts.Position.BuyPrice) / ts.Position.BuyPrice) * 100
}

// MarketValue returns the current total value of the position
func (ts *TickerState) MarketValue() float64 {
	ts.mu.RLock()
	defer ts.mu.RUnlock()
	return ts.CurrentPrice * ts.Position.Quantity
}

// Volatility returns the standard deviation of recent prices (simple risk proxy)
func (ts *TickerState) Volatility() float64 {
	ts.mu.RLock()
	defer ts.mu.RUnlock()

	if len(ts.PriceHistory) < 2 {
		return 0
	}

	prices := make([]float64, len(ts.PriceHistory))
	for i, p := range ts.PriceHistory {
		prices[i] = p.Price
	}

	// calculate mean
	var sum float64
	for _, p := range prices {
		sum += p
	}
	mean := sum / float64(len(prices))

	// calculate variance
	var variance float64
	for _, p := range prices {
		diff := p - mean
		variance += diff * diff
	}
	variance /= float64(len(prices))

	return math.Sqrt(variance)
}

// simulatePrice generates the next price using a random walk model
// This is a simplified geometric Brownian motion - the same math
// used in Black-Scholes options pricing
func simulatePrice(current float64, volatility float64) float64 {
	drift := 0.0001                              // tiny upward bias
	shock := volatility * rand.NormFloat64()     // random noise
	next := current * math.Exp(drift+shock)

	// clamp to avoid going to 0 or absurd values
	if next < 0.01 {
		next = 0.01
	}
	return next
}

// DefaultPortfolio returns a hardcoded demo portfolio
func DefaultPortfolio() []Position {
	return []Position{
		{Ticker: "AAPL", Quantity: 50, BuyPrice: 178.50, AlertDown: 3.0},
		{Ticker: "MSFT", Quantity: 30, BuyPrice: 415.20, AlertDown: 2.5},
		{Ticker: "NVDA", Quantity: 20, BuyPrice: 875.00, AlertDown: 4.0},
		{Ticker: "JPM",  Quantity: 40, BuyPrice: 198.75, AlertDown: 2.0},
		{Ticker: "TSLA", Quantity: 25, BuyPrice: 242.00, AlertDown: 5.0},
	}
}
