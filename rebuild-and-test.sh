#!/bin/bash
# Quick Fix and Rebuild Script for Student Grades Microservices

set -e  # Exit on error

echo "=== Student Grades Microservices - Rebuild Script ==="
echo ""

# Stop all running containers
echo "1. Stopping all containers..."
docker-compose down
echo "✅ Containers stopped"
echo ""

# Clean up old images (optional)
echo "2. Cleaning up old images..."
docker-compose down --rmi local
echo "✅ Old images removed"
echo ""

# Rebuild specific services that had errors
echo "3. Rebuilding auth-service and grade-calculation-service..."
docker-compose build --no-cache auth-service grade-calculation
echo "✅ Services rebuilt"
echo ""

# Start the services
echo "4. Starting services..."
docker-compose up -d postgres redis
echo "⏳ Waiting for database and Redis to be ready..."
sleep 10

docker-compose up -d auth-service grade-calculation grade-ingestion
echo "✅ Core services started"
echo ""

# Wait for services to be ready
echo "5. Waiting for services to start (30 seconds)..."
sleep 30
echo ""

# Test services
echo "6. Testing service health endpoints..."
echo ""

echo "Testing Auth Service (port 8080):"
curl -s http://localhost:8080/health && echo " ✅" || echo " ❌ Failed"
echo ""

echo "Testing Grade Ingestion Service (port 8081):"
curl -s http://localhost:8081/health && echo " ✅" || echo " ❌ Failed"
echo ""

echo "Testing Grade Calculation Service (port 8082):"
curl -s http://localhost:8082/health && echo " ✅" || echo " ❌ Failed"
echo ""

echo "=== Service Status ==="
docker-compose ps
echo ""

echo "=== Logs (last 20 lines) ==="
docker-compose logs --tail=20

echo ""
echo "✅ Setup complete!"
echo ""
echo "To view logs: docker-compose logs -f [service-name]"
echo "To stop all: docker-compose down"

