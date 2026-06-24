# Multi-stage build for Grade Calculation Service
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.1_12_1.9.7_3.3.1 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project project/

# Copy source code
COPY modules/common modules/common
COPY modules/grade-calculation modules/grade-calculation

# Build the service
RUN sbt "gradeCalculation/compile" "gradeCalculation/stage"

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifacts from builder
COPY --from=builder /app/modules/grade-calculation/target/universal/stage /app

# Expose the service port
EXPOSE 8082

# Run the service
CMD ["/app/bin/grade-calculation"]

