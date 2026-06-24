# Project Structure

## Directory Layout

```
students-grade-microservices/
├── build.sbt                          # Multi-module SBT build configuration
├── project/
│   ├── build.properties              # SBT version
│   └── plugins.sbt                   # SBT plugins (scalafmt, scalafix, etc.)
├── Makefile                          # Build automation commands
├── docker-compose.yml                # Infrastructure and services orchestration
├── .gitignore                        # Git ignore patterns
├── README.md                         # Main documentation
│
├── database/                         # Database schemas and migrations
│   ├── schema.sql                    # PostgreSQL schema
│   └── sample-data.sql               # Sample test data
│
├── docs/                             # Additional documentation
│   ├── ARCHITECTURE.md               # Architecture overview
│   └── API_TESTS.http                # HTTP API test collection
│
└── modules/                          # Microservices modules
    │
    ├── common/                       # Shared common module
    │   └── src/
    │       ├── main/scala/com/education/grades/common/
    │       │   ├── domain/           # Domain models
    │       │   │   ├── Models.scala  # Core entities
    │       │   │   └── Events.scala  # Domain events
    │       │   ├── auth/             # Authentication models
    │       │   │   └── Auth.scala    # Auth types
    │       │   ├── config/           # Configuration
    │       │   │   └── Config.scala  # App config
    │       │   ├── errors/           # Error types
    │       │   │   └── Errors.scala  # Error ADT
    │       │   ├── json/             # JSON codecs
    │       │   │   └── Codecs.scala  # Circe codecs
    │       │   └── infrastructure/   # Infrastructure layer
    │       │       ├── db/           # Database utilities
    │       │       ├── messaging/    # Redis messaging
    │       │       └── http/         # HTTP utilities
    │       └── test/scala/           # Common tests
    │
    ├── grade-ingestion/              # Grade Ingestion Service
    │   └── src/
    │       ├── main/
    │       │   ├── scala/com/education/grades/ingestion/
    │       │   │   ├── repository/   # Data access layer
    │       │   │   │   └── GradeRepository.scala
    │       │   │   ├── service/      # Business logic
    │       │   │   │   └── GradeIngestionService.scala
    │       │   │   ├── http/         # HTTP routes
    │       │   │   │   └── GradeIngestionRoutes.scala
    │       │   │   └── GradeIngestionServer.scala  # Main entry point
    │       │   └── resources/
    │       │       ├── application.conf  # Configuration
    │       │       └── logback.xml       # Logging config
    │       └── test/scala/           # Service tests
    │
    ├── grade-calculation/            # Grade Calculation Service
    │   └── src/
    │       ├── main/scala/com/education/grades/calculation/
    │       │   ├── service/          # Business logic
    │       │   │   └── GradeCalculationService.scala
    │       │   ├── http/             # HTTP routes
    │       │   │   └── GradeCalculationRoutes.scala
    │       │   └── GradeCalculationServer.scala
    │       └── test/scala/           # Service tests
    │
    ├── report-generation/            # Report Generation Service
    │   └── src/
    │       ├── main/scala/com/education/grades/reports/
    │       │   ├── service/          # Business logic
    │       │   │   └── ReportGenerationService.scala
    │       │   ├── http/             # HTTP routes
    │       │   │   └── ReportGenerationRoutes.scala
    │       │   └── ReportGenerationServer.scala
    │       └── test/scala/           # Service tests
    │
    ├── audit-logging/                # Audit Logging Service
    │   └── src/
    │       ├── main/scala/com/education/grades/audit/
    │       │   ├── service/          # Business logic
    │       │   │   └── AuditLoggingService.scala
    │       │   ├── http/             # HTTP routes
    │       │   │   └── AuditLoggingRoutes.scala
    │       │   └── AuditLoggingServer.scala
    │       └── test/scala/           # Service tests
    │
    └── auth-service/                 # Authentication Service
        └── src/
            ├── main/scala/com/education/grades/auth/
            │   ├── service/          # Business logic
            │   │   └── AuthService.scala
            │   ├── http/             # HTTP routes
            │   │   └── AuthRoutes.scala
            │   └── AuthServer.scala
            └── test/scala/           # Service tests
```

## Module Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                         common                               │
│  (domain models, errors, infrastructure, JSON codecs)       │
└─────────────────────────────────────────────────────────────┘
                          ▲
           ┌──────────────┼──────────────┐
           │              │              │
           │              │              │
┌──────────┴───────┐  ┌───┴──────┐  ┌───┴──────────────┐
│ grade-ingestion  │  │   auth   │  │ grade-calculation│
└──────────────────┘  └──────────┘  └──────────────────┘

           ▲              ▲              ▲
           │              │              │
    ┌──────┴──────┐  ┌────┴────┐  ┌─────┴────┐
    │report-gen   │  │ audit   │  │ (other)  │
    └─────────────┘  └─────────┘  └──────────┘
```

All services depend on the `common` module for shared code.

## Key Files and Their Purpose

### Build Configuration
- **build.sbt**: Multi-module SBT build with all dependencies
- **project/build.properties**: SBT version specification
- **project/plugins.sbt**: Build plugins (scalafmt, scalafix, native-packager)

### Infrastructure
- **docker-compose.yml**: Orchestrates PostgreSQL, Redis, and all microservices
- **database/schema.sql**: Complete database schema with all tables and indexes
- **database/sample-data.sql**: Sample data for testing

### Common Module
- **Models.scala**: Core domain entities (Student, Teacher, Course, Grade, etc.)
- **Events.scala**: Domain events for inter-service communication
- **Auth.scala**: Authentication and authorization models
- **Errors.scala**: Application error types
- **Codecs.scala**: JSON serialization/deserialization
- **Database.scala**: Database connection utilities
- **Messaging.scala**: Redis pub/sub utilities
- **Http.scala**: HTTP helpers and middleware

### Services
Each service follows a clean architecture pattern:

1. **Repository Layer** (`repository/`)
   - Data access logic
   - Skunk queries
   - Database interactions

2. **Service Layer** (`service/`)
   - Business logic
   - Domain operations
   - Event publishing/subscribing

3. **HTTP Layer** (`http/`)
   - REST API routes
   - Request/response handling
   - Authentication middleware

4. **Server** (`*Server.scala`)
   - Main entry point
   - Dependency wiring
   - Resource management

## Code Organization Patterns

### Package Structure
```scala
com.education.grades
├── common           // Shared across all services
│   ├── domain       // Domain models and value objects
│   ├── auth         // Authentication/authorization
│   ├── config       // Configuration types
│   ├── errors       // Error types
│   ├── json         // JSON codecs
│   └── infrastructure
│       ├── db       // Database utilities
│       ├── messaging// Event bus
│       └── http     // HTTP utilities
│
└── [service-name]   // Individual service
    ├── repository   // Data access
    ├── service      // Business logic
    └── http         // API routes
```

### Naming Conventions

**Domain Types:**
- Value classes for IDs: `StudentId`, `CourseId`, etc.
- Entities: `Student`, `Course`, `Grade`
- Enums: `UserRole`, `LetterGrade`, `AssignmentType`

**Repository Traits:**
- Pattern: `[Entity]Repository[F[_]]`
- Example: `GradeRepository[F[_]]`

**Service Traits:**
- Pattern: `[Feature]Service[F[_]]`
- Example: `GradeIngestionService[F[_]]`

**Routes:**
- Pattern: `[Feature]Routes[F[_]]`
- Example: `GradeIngestionRoutes[F[_]]`

**Server:**
- Pattern: `[Service]Server extends IOApp`
- Example: `GradeIngestionServer extends IOApp`

## Testing Structure

```
src/test/scala/
└── com/education/grades/[service]/
    ├── [Feature]ServiceSpec.scala    # Service layer tests
    ├── [Feature]RepositorySpec.scala # Repository tests
    └── [Feature]RoutesSpec.scala     # HTTP routes tests
```

## Configuration Files

**Application Configuration:**
- `modules/[service]/src/main/resources/application.conf`
- Uses Typesafe Config / PureConfig
- Environment variable overrides

**Logging Configuration:**
- `modules/[service]/src/main/resources/logback.xml`
- Structured logging
- Configurable log levels

## Running the Project

### Development
```bash
# Start infrastructure
make db-up

# Run specific service
sbt "project gradeIngestion" run

# Run tests
sbt test
```

### Production
```bash
# Start everything with Docker
make run-docker

# View logs
make logs

# Stop everything
docker-compose down
```

## Adding a New Service

1. Add module in `build.sbt`:
```scala
lazy val newService = (project in file("modules/new-service"))
  .settings(commonSettings)
  .dependsOn(common)
```

2. Create directory structure:
```bash
mkdir -p modules/new-service/src/main/scala/com/education/grades/newservice/{repository,service,http}
mkdir -p modules/new-service/src/test/scala
mkdir -p modules/new-service/src/main/resources
```

3. Implement:
   - Repository (data access)
   - Service (business logic)
   - Routes (HTTP API)
   - Server (main entry point)

4. Add to `docker-compose.yml`

5. Update documentation

## Best Practices

- **Keep services small and focused**
- **Share code only through common module**
- **Use events for inter-service communication**
- **Keep database schemas in version control**
- **Write tests for business logic**
- **Use type-safe database queries (Skunk)**
- **Handle errors with Either/EitherT**
- **Use Resource for proper cleanup**
- **Log important operations**
- **Validate input at API boundary**

