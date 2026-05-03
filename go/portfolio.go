package main

// Position represents a single holding in the portfolio
type Position struct {
	Ticker    string
	Quantity  float64
	BuyPrice  float64
	AlertDown float64
}

func DefaultPortfolio() []Position {
	return []Position{
		{Ticker: "AAPL", Quantity: 50, BuyPrice: 178.50, AlertDown: 3.0},
		{Ticker: "MSFT", Quantity: 30, BuyPrice: 415.20, AlertDown: 2.5},
		{Ticker: "NVDA", Quantity: 20, BuyPrice: 875.00, AlertDown: 4.0},
		{Ticker: "JPM",  Quantity: 40, BuyPrice: 198.75, AlertDown: 2.0},
		{Ticker: "TSLA", Quantity: 25, BuyPrice: 242.00, AlertDown: 5.0},
	}
}
