# Rankify API - GitHub Copilot Instructions

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

Rankify API is a Spring Boot API application (Java 24) for ranked voting built with Maven, PostgreSQL, and containerized development environment using Docker/Podman compose.

## Core Architecture Principles

**CRITICAL - Follow these principles in every code change:**

### Hexagonal Architecture (Ports & Adapters)
- **3 Layers**: `facade/` (HTTP/REST) → `business/` (domain logic) → `data/` (persistence)
- **Dependency Rule**: Business layer has ZERO dependencies on infrastructure. Data layer depends on business interfaces (dependency inversion).
- **Business Layer**: Pure domain logic - entities, value objects, use cases. No Spring/JPA/HTTP knowledge.
- **Repository Pattern**: Business defines interfaces (ports), data implements them (adapters).

### Domain-Driven Design
- **Entities**: Have identity (e.g., `Poll` with `PollId`), mutable lifecycle, aggregate roots
- **Value Objects**: Immutable, no identity (e.g., `Option`, `PollTitle` - use records)
- **Aggregates**: Access children only through aggregate root (e.g., votes through `Poll.castVote()`)
- **Ubiquitous Language**: Use `businessModel.md` terminology in code - no technical jargon in business layer

### Clean Code Essentials
- Immutable value objects, explicit domain operations, fail-fast validation
- Wrap primitives in value objects (no primitive obsession)
- One use case per business operation, business logic in domain objects not services

**Package Structure**: `{context}/business/`, `{context}/data/`, `{context}/facade/`  
**Naming**: `*UseCase` (business), `*Controller` (facade), `*Entity` (data)  
**Reference**: See `businessModel.md` for complete domain model

## Working Effectively

### Bootstrap, Build, and Test

**Critical Build Requirements:**
- Project requires Java 24 and Maven 3.9.9+
- Build uses containerized Maven environment for Java 24 compatibility
- In sandboxed/CI environments, falls back to local Maven (may fail with Java version errors)

**Build Commands (NEVER CANCEL - Wait for completion):**
```bash
# Containerized build (recommended for Java 24 compatibility)
./build.sh                              # Takes 2-5 minutes, NEVER CANCEL
# or specify Maven goals
./build.sh "clean install"              # Takes 2-5 minutes, NEVER CANCEL

# Local Maven (only works with Java 17+ environments)
mvn clean install                       # Takes 45-60 seconds, NEVER CANCEL
```

**Container Build:**
```bash
# Build application container (requires successful Maven build first)
docker build -f Containerfile -t rankify-api:latest .    # Takes 3-10 seconds, NEVER CANCEL
# or with Podman
podman build -f Containerfile -t rankify-api:latest .    # Takes 3-10 seconds, NEVER CANCEL
```

**Test Execution:**
```bash
# Run unit tests (part of `mvn clean install`)
mvn test                               # Takes 5-10 seconds, NEVER CANCEL
```

### One-Command Environment Setup

**Complete development environment setup:**
```bash
./setup.sh                            # Takes 8-15 seconds, NEVER CANCEL
```

This script:
1. Builds the application using containerized Maven
2. Creates application container image  
3. Starts PostgreSQL database and application via compose

**Manual Environment Setup (Alternative):**
```bash
# Step 1: Build application
./build.sh                            # Takes 2-5 minutes, NEVER CANCEL

# Step 2: Build container
docker build -f Containerfile -t rankify-api:latest .    # Takes 3-10 seconds, NEVER CANCEL

# Step 3: Start environment
docker compose up -d                   # Takes 8-15 seconds, NEVER CANCEL
```

### Run the Application

**Container Environment (Recommended):**
```bash
# Start complete environment with database
docker compose up -d                   # Takes 8-15 seconds, NEVER CANCEL

# View application logs
docker compose logs -f app

# View database logs  
docker compose logs -f database

# Stop environment
docker compose down
```

**Standalone Database Testing:**
```bash
# Start PostgreSQL only for local development
docker run -d --name rankify-db \
    -e POSTGRES_DB=rankify \
    -e POSTGRES_USER=rankify \
    -e POSTGRES_PASSWORD=rankify \
    -p 5432:5432 \
    postgres:16-alpine                 # Takes 3-5 seconds, NEVER CANCEL
```

## Validation and Testing

**Always test application functionality after making changes:**

### Manual Validation Requirements

**CRITICAL: After building and running, you MUST test actual functionality:**

1. **Health Check Validation:**
   ```bash
   # Wait for services to start (60+ seconds), then test
   curl http://localhost:8080/actuator/health
   # Expected response: {"status":"UP"}
   ```

2. **Database Connectivity Test:**
   ```bash
   # Test PostgreSQL is accepting connections
   docker exec rankify-database pg_isready -U rankify -d rankify
   # Expected: "/var/run/postgresql:5432 - accepting connections"
   ```

3. **Service Status Check:**
   ```bash
   docker compose ps
   # Both services should show "healthy" status after startup period
   ```

**Validation Scenarios:**
- Always test the complete startup sequence: build → containerize → compose up → health check
- Test individual component isolation (database only, application only)
- Verify logging outputs for both application and database services
- Check network connectivity between containers in compose environment

## Environment Configuration

**Development Configuration:**
- **Database**: PostgreSQL 16 running in container
- **Database Access**: `localhost:5432`, user: `rankify`, password: `rankify`, database: `rankify`
- **Application**: Spring Boot with `dev` profile active
- **Application Access**: http://localhost:8080
- **Health Endpoint**: http://localhost:8080/actuator/health
- **Container Networking**: Services communicate via `rankify-network`

## Known Limitations and Workarounds

**Sandboxed/CI Environments:**
- Containerized builds may fail due to certificate/networking issues
- Falls back to local Maven automatically (requires Java 17+ compatibility)
- Container networking may have DNS resolution issues between containers
- Individual components (database, application) can be tested separately

**Build System Compatibility:**
- Build scripts are designed for Git Bash on Windows but work on Linux
- Use `CONTAINER_ENGINE=docker` environment variable to force Docker instead of Podman
- Path conversion issues may occur in mixed Windows/Linux environments

**Networking Issues:**
```bash
# If container networking fails, use host networking for testing
docker run --network host -e SPRING_PROFILES_ACTIVE=dev \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rankify \
    rankify-api:latest
```

## Common Tasks and Commands

### Development Workflow Commands

**After code changes:**
```bash
# Rebuild and restart application
./build.sh && docker build -f Containerfile -t rankify-api:latest . && docker compose up -d app
# Takes 2-8 minutes total, NEVER CANCEL
```

**Debugging and Monitoring:**
```bash
# View all service logs
docker compose logs -f

# Check service status
docker compose ps

# Restart specific service
docker compose restart app
docker compose restart database

# Shell access to application container
docker compose exec app sh

# Shell access to database container
docker compose exec database psql -U rankify -d rankify
```

### Testing and Validation Commands

**Component Testing:**
```bash
# Test environment components individually
./test-environment.sh                 # Takes 30-60 seconds, NEVER CANCEL
```

**Manual Integration Testing:**
```bash
# Test complete workflow
./setup.sh                           # Takes 8-15 seconds, NEVER CANCEL
sleep 30                              # Wait for services to be ready
curl http://localhost:8080/actuator/health
docker compose logs app | tail -20   # Check for errors
```

## Project Structure

### Key Files and Directories

**Build and Configuration:**
- `pom.xml` - Maven project configuration (Java 24, Spring Boot 3.4.1)
- `build.sh` - Containerized Maven build script
- `setup.sh` - One-command environment setup
- `test-environment.sh` - Component validation script
- `Containerfile` - Application container definition
- `compose.yml` - Development environment orchestration

**Application Code:**
- `src/main/java/de/tonypsilon/rankify/api/` - Main application code
- `src/main/resources/application-dev.yml` - Development configuration
- `src/test/java/` - Unit tests

**Documentation:**
- `readme.md` - Detailed setup and usage instructions
- `businessModel.md` - Domain-driven business model specification  
- `technicalDatamodel.md` - Database schema and technical implementation

### Application Architecture

**Domain Model:** Domain-Driven Design (DDD) approach for ranked voting:
- **Poll** (Entity) - Main aggregate root with lifecycle states
- **Option** (Value Object) - Voting choices within polls
- **Vote** (Value Object) - User rankings of options
- **PollResult** (Value Object) - Calculated voting results

**Technology Stack:**
- Java 24 with Spring Boot 3.4.1
- Spring Data JPA with Hibernate
- PostgreSQL 16 database
- Spring Boot Actuator for monitoring
- Maven 3.9.9 build system
- Docker/Podman containerization

## Timeout Settings and Performance

**Critical Timing Information:**

| Operation | Expected Time | Minimum Timeout | Status |
|-----------|---------------|-----------------|---------|
| Maven Build (containerized) | 2-5 minutes | 10 minutes | NEVER CANCEL |
| Maven Build (local) | 45-60 seconds | 5 minutes | NEVER CANCEL |
| Container Build | 3-10 seconds | 2 minutes | NEVER CANCEL |
| Compose Up | 8-15 seconds | 5 minutes | NEVER CANCEL |
| Application Startup | 5-10 seconds | 3 minutes | NEVER CANCEL |
| Database Startup | 3-5 seconds | 2 minutes | NEVER CANCEL |
| Health Check Response | Immediate | 30 seconds | After 60s warmup |

**NEVER CANCEL builds or long-running commands.** Build processes may appear to hang but are downloading dependencies or waiting for container operations to complete.

## Final Notes

- Always run validation commands after making changes
- Use the health endpoint to verify application status before testing functionality
- Check both application and database logs when troubleshooting issues
- The containerized development environment is the canonical setup - local alternatives may have compatibility issues
- In production environments, full container orchestration works as designed despite sandbox limitations