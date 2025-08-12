# Rankify API

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
