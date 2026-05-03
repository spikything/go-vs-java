# Stage 1: build — uses full Go toolchain
FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod .
COPY *.go .
# CGO_ENABLED=0 produces a fully static binary — no libc dependency
# This is what lets us use the scratch base image below
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o monitor .

# Stage 2: run — scratch is literally an empty image, just the binary
# Final image size: ~6MB vs ~200MB for a Java equivalent
FROM scratch
COPY --from=builder /app/monitor /monitor
ENTRYPOINT ["/monitor"]
