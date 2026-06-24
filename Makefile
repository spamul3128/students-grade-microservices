# Makefile for Students Grade Microservices

.PHONY: help build test run-local run-docker clean db-up db-down

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ============================================================
# Quick Scripts (Most Used)
# ============================================================

menu: ## Open interactive menu for all operations
	@./scripts/run.sh

setup: ## Setup development environment
	@./scripts/dev-setup.sh

deploy: ## Deploy all services with Docker
	@./scripts/deploy-docker.sh --detach

test-api: ## Run automated API tests
	@./scripts/test-api.sh

health: ## Check health of all services
	@./scripts/health-check.sh

db-count: ## Show database record counts
	@./scripts/db-count.sh

validate: ## Validate all scripts and configuration
	@./scripts/validate.sh

# ============================================================
# Build & Compile
# ============================================================

build: ## Compile all modules
	sbt compile

test: ## Run all tests
	sbt test

format: ## Format code with scalafmt
	sbt scalafmt

lint: ## Run scalafix linter
	sbt scalafix

clean: ## Clean build artifacts
	sbt clean
	docker-compose down -v

# ============================================================
# Database Management
# ============================================================

db-up: ## Start database and Redis
	docker-compose up -d postgres redis

db-down: ## Stop database and Redis
	docker-compose down postgres redis

db-clean: ## Clean database tables
	@./scripts/db-cleanup.sh --clean

db-reset: ## Reset database (drop & recreate)
	@./scripts/db-cleanup.sh --reset

db-restart: ## Restart database and Redis
	@./scripts/db-restart.sh

# ============================================================
# Service Management
# ============================================================

run-local: db-up ## Run all services locally
	@echo "Starting all services..."
	@echo "You need to run each service in a separate terminal:"
	@echo "  Terminal 1: sbt 'project authService' run"
	@echo "  Terminal 2: sbt 'project gradeIngestion' run"
	@echo "  Terminal 3: sbt 'project gradeCalculation' run"
	@echo "  Terminal 4: sbt 'project reportGeneration' run"
	@echo "  Terminal 5: sbt 'project auditLogging' run"

run-docker: ## Build and run all services with Docker
	docker-compose up --build

run-docker-detach: ## Build and run all services with Docker in detached mode
	docker-compose up --build -d

stop: ## Stop all services
	@./scripts/stop-all.sh

# ============================================================
# Monitoring & Logs
# ============================================================

logs: ## Show logs from all Docker services
	@./scripts/logs.sh

logs-auth: ## Show auth service logs
	@./scripts/logs.sh --service auth-service

logs-db: ## Show PostgreSQL logs
	@./scripts/logs.sh --service postgres

ps: ## Show status of all Docker services
	docker-compose ps

# ============================================================
# Database Access
# ============================================================

shell-postgres: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U postgres -d grades_db

shell-redis: ## Open Redis CLI
	docker-compose exec redis redis-cli

# ============================================================
# Packaging & Building
# ============================================================

package: ## Package all services
	sbt assembly

docker-build: ## Build Docker images for all services
	docker-compose build

coverage: ## Run tests with coverage
	sbt clean coverage test coverageReport

