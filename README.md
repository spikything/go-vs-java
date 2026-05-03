# Go vs Java

A basic fake stock tracking CLI application, written in both Java ans Go, to compare to two for simplicity, concurrency performance, memory efficiency, etc.

## What it does

- Simulates live price movement for a 5-stock portfolio using geometric Brownian motion (simplified Black-Scholes noise)
- Monitors each ticker concurrently - one goroutine/thread per ticker
- Fires drawdown alerts when a position drops below its configured threshold
- Renders a live-updating terminal dashboard with P&L, market value, volatility (σ), and a risk exposure bar chart
- Shuts down cleanly on Ctrl+C

## Running it

### Go

```bash
cd go
go run .
```

Or build a binary:

```bash
go build -o monitor .
./monitor
```

### Java

```bash
cd java
mvn package -q
java -jar target/stock-risk-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Docker (the real comparison)

```bash
# Go - scratch image, ~6MB
docker build -t risk-monitor-go ./go
docker run -it risk-monitor-go

# Java - JRE image, ~200MB
docker build -t risk-monitor-java ./java
docker run -it risk-monitor-java

# Compare image sizes
docker images | grep risk-monitor
```

## Benchmarks

Run these yourself and fill in the real numbers - that's the point.

| Metric                | Go                                    | Java                                          |
| --------------------- | ------------------------------------- | --------------------------------------------- |
| **Startup time**      | `time ./monitor`                      | `time java -jar app.jar`                      |
| **Idle memory (RSS)** | `ps aux \| grep monitor`              | `ps aux \| grep java`                         |
| **Docker image size** | `docker images risk-monitor-go`       | `docker images risk-monitor-java`             |
| **Lines of code**     | `find go -name '*.go' \| xargs wc -l` | `find java/src -name '*.java' \| xargs wc -l` |

Expected results (approximate):

| Metric        | Go   | Java       |
| ------------- | ---- | ---------- |
| Startup       | ~5ms | ~250-400ms |
| Idle RSS      | ~8MB | ~80-150MB  |
| Docker image  | ~6MB | ~180-200MB |
| Lines of code | ~250 | ~380       |

## Code structure

```
go/
  main.go        - entrypoint, shutdown handling, wiring
  monitor.go     - concurrent engine: goroutines, channels, select
  portfolio.go   - data types, price simulation, default positions
  ui.go          - ANSI terminal renderer

java/
  src/main/java/com/riskmonitor/
    Main.java          - entrypoint, shutdown hook
    Monitor.java       - ScheduledExecutorService, concurrent price engine
    TickerState.java   - mutable state with ReadWriteLock
    Position.java      - portfolio position data
    Alert.java         - alert value object
    TickerSnapshot.java - point-in-time snapshot for UI
    UI.java            - ANSI terminal renderer
```

## Key language comparisons

### Concurrency

**Go** - goroutines are ~4KB and managed by the runtime:

```go
go monitor.Run(done, alertCh)  // that's it
```

**Java** - requires explicit thread pool configuration:

```java
ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
scheduler.scheduleAtFixedRate(() -> updateTicker(...), 0, 500, TimeUnit.MILLISECONDS);
```

### Shutdown signalling

**Go** - channels are first-class:

```go
done := make(chan struct{})
close(done)  // broadcasts to all goroutines
```

**Java** - shutdown hooks or volatile flags:

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> monitor.stop()));
```

### Lock management

**Go** - `defer` makes unlock guaranteed and visible:

```go
ts.mu.Lock()
defer ts.mu.Unlock()
// ... do work
```

**Java** - `try/finally` required, more ceremony:

```java
lock.writeLock().lock();
try {
    // ... do work
} finally {
    lock.writeLock().unlock();
}
```

### Deployment

**Go** - single static binary, no runtime required:

```dockerfile
FROM scratch
COPY --from=builder /app/monitor /monitor
```

**Java** - needs JRE, ~200MB minimum image:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
```
