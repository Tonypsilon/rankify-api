# Rankify API

[![CI Build and Test](https://github.com/Tonypsilon/rankify-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Tonypsilon/rankify-api/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Tonypsilon/rankify-api/branch/main/graph/badge.svg)](https://codecov.io/gh/Tonypsilon/rankify-api)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Tonypsilon_rankify-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Tonypsilon_rankify-api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Tonypsilon_rankify-api&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=Tonypsilon_rankify-api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Tonypsilon_rankify-api&metric=maintainability_rating)](https://sonarcloud.io/summary/new_code?id=Tonypsilon_rankify-api)

API application of the rankify project, an app for ranked voting.

## Quick Start Development Environment

This project provides a machine-agnostic development environment that works on Windows, Linux, and Mac with minimal setup requirements.

### Prerequisites

- **IntelliJ IDEA Ultimate** (or any IDE of your choice)
- **Container engine**: Podman (recommended) or Docker
- **Optional**: podman compose or docker-compose for easier orchestration

### One-Command Setup

```bash
./setup.sh
```

This single command will:
1. Build the application using a containerized Maven environment
2. Create the application container image
3. Start both the PostgreSQL database and the application

After setup, your development environment will be running with:
- **Application**: http://localhost:8080
- **Health check**: http://localhost:8080/actuator/health  
- **Database**: localhost:5432 (user: `rankify`, password: `rankify`, database: `rankify`)

### Manual Setup (Alternative)

If you prefer manual control over the process:

1. **Build the application**:
   ```bash
   ./build.sh
   ```

2. **Build the application container**:
   ```bash
   podman build -f Containerfile -t rankify-api:latest .
   ```

3. **Start the environment**:
   ```bash
   podman compose up -d
   # or: docker-compose up -d
   ```

### Development Workflow

#### Making Code Changes
After modifying the code, rebuild and restart:
```bash
./build.sh && podman build -f Containerfile -t rankify-api:latest . && podman compose up -d app
```

#### Useful Commands
```bash
# View application logs
podman compose logs -f app

# View database logs  
podman compose logs -f database

# Stop the environment
podman compose down

# Restart services
podman compose restart

# Check service status
podman compose ps
```

### Environment Configuration

The development environment uses the `dev` Spring profile with the following configuration:
- Database host: `database` (container name)
- Database port: `5432`
- Database name: `rankify`
- Database credentials: `rankify` / `rankify`

### Database Schema Management

This project uses **Liquibase** as the database changelog management tool with YAML-based changesets. The database schema is version-controlled and automatically applied during application startup.

**Changelog Structure:**
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Individual changesets: `src/main/resources/db/changelog/changes/`

**Key Features:**
- Automatic schema creation and migration on startup
- Version-controlled database changes
- Rollback capabilities for schema changes
- Cross-environment consistency (dev, test, prod)

Hibernate's DDL auto-generation is disabled (`ddl-auto: none`) in favor of Liquibase-managed schema evolution.

### Extending the Environment

The compose.yml file is designed to be extensible. Future additions (like a frontend application) can be easily added as new services to the existing configuration.

### Technical Details

- **Java Version**: 24
- **Spring Boot Version**: 3.4.1
- **Database**: PostgreSQL 16
- **Database Changelog**: Liquibase (YAML-based)
- **Container Base**: Eclipse Temurin Alpine Linux
- **Build Tool**: Maven 3.9.9 (containerized)

### CI/CD Pipeline

This project includes a comprehensive CI/CD pipeline using GitHub Actions that ensures code quality and security:

**Automated Workflows:**
- **Continuous Integration**: Runs on every push and pull request to `main` and `develop` branches
- **Security Scanning**: Weekly dependency vulnerability scans and security analysis

**CI Workflow Features:**
- ✅ **Java 24 Environment**: Uses Eclipse Temurin JDK 24 for consistency with development environment
- ✅ **TestContainers Integration**: Automatically spins up PostgreSQL containers for integration tests
- ✅ **Test Coverage**: JaCoCo generates coverage reports with 80%+ target
- ✅ **Static Code Analysis**: SonarQube analysis for code quality, security, and maintainability
- ✅ **Dependency Caching**: Maven dependencies cached for faster builds
- ✅ **Artifact Uploads**: Test results, coverage reports, and Docker images stored as artifacts
- ✅ **Docker Build**: Application containerized and validated on main branch pushes

**Security Scanning:**
- 🔒 **OWASP Dependency Check**: Scans for known vulnerabilities in dependencies
- 🔒 **Trivy Security Scan**: Filesystem vulnerability scanning with SARIF upload
- 🔒 **Automated Security Alerts**: Integration with GitHub Security tab

**Quality Gates:**
- All tests must pass before merge
- Coverage reports uploaded to Codecov
- SonarQube quality gates enforce maintainability standards
- Security vulnerabilities prevent builds with CVSS score ≥ 7

**Setup Requirements:**
To enable full CI/CD functionality, configure these repository secrets:
- `SONAR_TOKEN`: SonarCloud authentication token for static analysis
- `CODECOV_TOKEN`: (Optional) Codecov token for enhanced coverage reporting

### Troubleshooting

**Windows Compatibility**: The build and setup scripts are compatible with Git Bash on Windows. They use relative paths to ensure cross-platform compatibility and avoid Windows path conversion issues.

**Build Issues**: The build process uses Java 24 in a container. If you encounter Java version issues, ensure you're using the containerized build script (`./build.sh`) rather than local Maven.

**Container Engine**: By default, the scripts use Podman. To use Docker instead, set the environment variable:
```bash
export CONTAINER_ENGINE=docker
./setup.sh
```

**Sandbox/CI Environments**: In some sandboxed or CI environments, container networking may have limitations that prevent proper DNS resolution between containers. In such cases:
- The containerized build process will fall back to using local Maven if available
- The individual components (database, application container) can be tested separately
- In production environments, the full container orchestration works as designed

**Health Checks**: The application includes health checks that verify both the application and database are ready before marking services as healthy.

**Network Connectivity**: If you experience container networking issues:
1. Ensure your container engine supports inter-container communication
2. Check firewall settings that might block container traffic
3. Try using Docker instead of Podman if networking issues persist
4. Verify that ports 5432 and 8080 are available on your system

## Architecture

### Domain-Driven Design (DDD)

The Rankify API follows Domain-Driven Design principles with clear separation of concerns:

- **Entities**: `Poll` - Aggregates with identity and lifecycle management
- **Value Objects**: `Vote`, `Option`, `Ballot`, `PollId`, `PollTitle`, etc. - Immutable objects without identity
- **Domain Services**: `VoteFactory` - Encapsulates complex creation logic

#### Vote Casting Architecture

The vote casting mechanism follows best practices for DDD:

**Components**:
- **Vote Interface** - Public contract for votes (only exposes `pollId()` and `rankings()`)
- **RecordedVote** - Package-private implementation ensuring encapsulation
- **VoteFactory** - Domain service responsible for creating valid votes
- **Poll.canAcceptVotes()** - Query method to check poll state
- **CastVoteUseCase** - Application service orchestrating the process

**Design Rationale**:
- **Factory Pattern**: Vote creation is delegated to `VoteFactory` instead of being embedded in the Poll entity
- **Single Responsibility**: Poll focuses on its lifecycle; VoteFactory handles vote creation
- **Encapsulation**: RecordedVote is package-private; only accessible through the factory
- **Validation Centralization**: All vote validation logic is in one place (VoteFactory)

**Usage Example**:
```java
@Service
public class MyService {
    private final VoteFactory voteFactory;
    private final PollRepository pollRepository;
    
    public void castVote(PollId pollId, Map<Option, Integer> rankings) {
        Poll poll = pollRepository.getById(pollId);
        Vote vote = voteFactory.createVote(poll, rankings);
        pollRepository.saveVote(vote);
    }
}
```

For detailed analysis of the vote casting design, see:
- `VOTE_CASTING_DESIGN_ANALYSIS.md` - Comprehensive DDD/Clean Architecture analysis
- `VOTE_CASTING_REFACTORING_SUMMARY.md` - Summary of changes and migration guide
- `businessModel.md` - Business domain model specification
