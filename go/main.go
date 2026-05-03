package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	fmt.Println("Starting Stock Risk Monitor (Go)...")

	// set up graceful shutdown - listens for Ctrl+C or SIGTERM
	// channels are the idiomatic Go way to signal across goroutines
	done := make(chan struct{})
	alertCh := make(chan Alert, 50) // buffered so monitor never blocks on UI

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	// initialise the monitor with the demo portfolio
	portfolio := DefaultPortfolio()
	monitor := NewMonitor(portfolio)
	ui := NewUI()

	// start all ticker goroutines in the background
	// this single line kicks off 5 concurrent goroutines
	go monitor.Run(done, alertCh)

	// start the UI refresh loop - 500ms feels live without flickering
	go func() {
		ticker := time.NewTicker(500 * time.Millisecond)
		defer ticker.Stop()
		for {
			select {
			case <-done:
				return
			case <-ticker.C:
				snap := monitor.Snapshot()
				alerts := monitor.RecentAlerts(10)
				ui.Render(snap, alerts)
			}
		}
	}()

	// block until signal received - clean and explicit
	<-sigCh
	fmt.Print("\n\nShutting down...")
	close(done) // broadcasts to all goroutines
	ui.Close()
	fmt.Println(" done.")
}
