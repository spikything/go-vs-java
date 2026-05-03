package main

import (
	"fmt"
	"math"
	"sort"
	"strings"
	"time"
)

// ANSI escape codes - no external deps needed for a clean terminal UI
const (
	reset     = "\033[0m"
	bold      = "\033[1m"
	dim       = "\033[2m"
	red       = "\033[31m"
	green     = "\033[32m"
	yellow    = "\033[33m"
	cyan      = "\033[36m"
	white     = "\033[37m"
	bgRed     = "\033[41m"
	clearLine = "\033[2K"
	moveUp    = "\033[%dA" // move cursor up N lines
	hideCursor = "\033[?25l"
	showCursor = "\033[?25h"
)

// UI manages the terminal rendering
type UI struct {
	startTime   time.Time
	lastAlerts  []Alert
	frameCount  int
	totalLines  int // track how many lines we drew last frame
}

// NewUI creates a UI instance
func NewUI() *UI {
	fmt.Print(hideCursor)
	return &UI{startTime: time.Now()}
}

// Close restores the terminal
func (u *UI) Close() {
	fmt.Print(showCursor)
	fmt.Println()
}

// Render draws a full frame of the dashboard
func (u *UI) Render(snap map[string]TickerSnapshot, alerts []Alert) {
	u.frameCount++

	// move cursor up to overwrite previous frame
	if u.totalLines > 0 {
		fmt.Printf("\033[%dA", u.totalLines)
	}

	var sb strings.Builder
	lines := 0

	write := func(s string) {
		sb.WriteString(clearLine + s + "\n")
		lines++
	}

	// ── Header ──────────────────────────────────────────────────────────
	uptime := time.Since(u.startTime).Round(time.Second)
	write(fmt.Sprintf("%s%s  PORTFOLIO RISK MONITOR%s  %s(Go)%s  uptime: %s  frame: %d",
		bold, cyan, reset, dim, reset, uptime, u.frameCount))
	write(strings.Repeat("─", 72))

	// ── Portfolio table ──────────────────────────────────────────────────
	write(fmt.Sprintf("%s%-6s  %8s  %8s  %10s  %8s  %8s  %6s%s",
		bold, "TICKER", "PRICE", "BUY", "MKT VALUE", "P&L $", "P&L %", "VOL σ", reset))
	write(strings.Repeat("─", 72))

	// sort tickers alphabetically for stable display
	tickers := make([]string, 0, len(snap))
	for t := range snap {
		tickers = append(tickers, t)
	}
	sort.Strings(tickers)

	var totalPnL, totalValue float64
	for _, t := range tickers {
		s := snap[t]
		totalPnL += s.PnL
		totalValue += s.MarketValue

		pnlColor := green
		sign := "+"
		if s.PnL < 0 {
			pnlColor = red
			sign = ""
		}

		// mini sparkline using block chars - shows price direction
		sparkChar := "▲"
		if s.PnLPct < 0 {
			sparkChar = "▼"
		}

		write(fmt.Sprintf("%s%-6s%s  %8.2f  %8.2f  %10.2f  %s%s%.2f%s  %s%s%.2f%%%s  %6.3f",
			bold, t, reset,
			s.CurrentPrice,
			s.BuyPrice,
			s.MarketValue,
			pnlColor, sign, s.PnL, reset,
			pnlColor, sparkChar+" ", s.PnLPct, reset,
			s.Volatility,
		))
	}

	// ── Totals ───────────────────────────────────────────────────────────
	write(strings.Repeat("─", 72))
	pnlColor := green
	sign := "+"
	if totalPnL < 0 {
		pnlColor = red
		sign = ""
	}
	write(fmt.Sprintf("%sTOTAL%s  %s%33.2f  %s%s%s%.2f%s",
		bold, reset,
		dim, totalValue, reset,
		pnlColor+bold, sign, totalPnL, reset))

	// ── Risk summary bar ─────────────────────────────────────────────────
	write("")
	write(bold + "RISK EXPOSURE" + reset)
	for _, t := range tickers {
		s := snap[t]
		pct := (s.MarketValue / totalValue) * 100
		barLen := int(math.Round(pct / 2)) // max 50 chars for 100%
		bar := strings.Repeat("█", barLen) + strings.Repeat("░", 50-barLen)
		write(fmt.Sprintf("  %-6s %s%s%s %.1f%%", t, cyan, bar, reset, pct))
	}

	// ── Alerts ───────────────────────────────────────────────────────────
	write("")
	write(bold + "RECENT ALERTS" + reset)
	if len(alerts) == 0 {
		write(dim + "  No alerts fired yet." + reset)
	} else {
		// show last 5, newest first
		start := 0
		if len(alerts) > 5 {
			start = len(alerts) - 5
		}
		for i := len(alerts) - 1; i >= start; i-- {
			a := alerts[i]
			ts := a.Timestamp.Format("15:04:05")
			write(fmt.Sprintf("  %s[%s]%s %s%-6s%s  %s",
				dim, ts, reset, red+bold, a.Ticker, reset, a.Message))
		}
	}

	write(strings.Repeat("─", 72))
	write(dim + "  Ctrl+C to exit" + reset)

	fmt.Print(sb.String())
	u.totalLines = lines
}
