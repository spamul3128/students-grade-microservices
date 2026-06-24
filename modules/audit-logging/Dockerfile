# Multi-stage build for Audit Logging Service
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.1_12_1.9.7_3.3.1 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project project/

# Copy source code
COPY modules/common modules/common
COPY modules/audit-logging modules/audit-logging

# Build the service
RUN sbt "auditLogging/compile" "auditLogging/stage"

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifacts from builder
COPY --from=builder /app/modules/audit-logging/target/universal/stage /app

# Expose the service port
EXPOSE 8084

# Run the service
CMD ["/app/bin/audit-logging"]

