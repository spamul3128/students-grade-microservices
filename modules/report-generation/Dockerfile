# Multi-stage build for Report Generation Service
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.1_12_1.9.7_3.3.1 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project project/

# Copy source code
COPY modules/common modules/common
COPY modules/report-generation modules/report-generation

# Build the service
RUN sbt "reportGeneration/compile" "reportGeneration/stage"

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifacts from builder
COPY --from=builder /app/modules/report-generation/target/universal/stage /app

# Expose the service port
EXPOSE 8083

# Run the service
CMD ["/app/bin/report-generation"]

