package main

import "fmt"

func main() {
	fmt.Println("Starting Stock Risk Monitor (Go)...")

	portfolio := DefaultPortfolio()
	for _, p := range portfolio {
		fmt.Printf("  %s: %.0f shares @ $%.2f\n", p.Ticker, p.Quantity, p.BuyPrice)
	}
}
