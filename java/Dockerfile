# Stage 1: build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# download deps first (cached layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -q -DskipTests

# Stage 2: run
# eclipse-temurin is the smallest production-ready JRE image
# Still ~200MB vs Go's ~6MB scratch image - that's the point
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/stock-risk-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# JVM startup flags:
# -Xms8m -Xmx64m  - constrain heap for a fair comparison with Go's ~8MB
# -XX:+UseSerialGC - smaller GC footprint for a single-core container
ENTRYPOINT ["java", "-Xms8m", "-Xmx64m", "-XX:+UseSerialGC", "-jar", "app.jar"]
