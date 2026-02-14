# Rankify API - GitHub Copilot Instructions

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

Rankify API is a Spring Boot API application (Java 24) for ranked voting built with Maven, PostgreSQL, and containerized development environment using Docker/Podman compose.

## Core Principles

This project strictly follows:
- **Clean Code** - Readable, maintainable, self-documenting code
- **Hexagonal Architecture (Ports & Adapters)** - Domain logic isolated from infrastructure
- **Domain-Driven Design (DDD)** - Business domain drives technical implementation

**CRITICAL**: Always prioritize these principles over convenience or shortcuts. When in doubt, favor explicit domain modeling over technical optimizations.

## Architecture & Design Principles

### Hexagonal Architecture (Ports & Adapters)

The application follows hexagonal architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                         FACADE LAYER                         │
│  (REST Controllers, DTOs, JSON Serialization)               │
│  → Translates HTTP requests to domain operations            │
└──────────────────────┬──────────────────────────────────────┘
                       │ Dependency: facade → business
┌──────────────────────▼──────────────────────────────────────┐
│                        BUSINESS LAYER                        │
│  (Domain Model, Use Cases, Business Logic)                  │
│  → Pure domain logic, no infrastructure dependencies        │
│  → Entities, Value Objects, Domain Services                 │
└──────────────────────┬──────────────────────────────────────┘
                       │ Dependency: business → data (via ports)
┌──────────────────────▼──────────────────────────────────────┐
│                          DATA LAYER                          │
│  (JPA Entities, Repositories, Database Mappers)             │
│  → Implements repository ports from business layer          │
│  → Translates between domain and persistence models         │
└─────────────────────────────────────────────────────────────┘
```

**Layer Responsibilities:**

**Facade Layer** (`poll/facade/`)
- REST controllers handling HTTP requests/responses
- Command objects (DTOs) for input validation
- JSON serialization/deserialization
- HTTP status code mapping
- **MUST NOT** contain business logic
- **MUST NOT** directly access data layer
- **MAY** call multiple use cases to compose operations

**Business Layer** (`poll/business/`)
- Domain entities (e.g., `Poll`, `Vote`, `Ballot`)
- Value objects (e.g., `PollId`, `Option`, `PollTitle`)
- Use cases (e.g., `InitiatePollUseCase`, `CastVoteUseCase`)
- Business rules and invariants
- Domain exceptions
- **MUST NOT** depend on Spring framework (except minimal annotations)
- **MUST NOT** know about HTTP, JSON, or database details
- **MAY** define repository interfaces (ports) that data layer implements

**Data Layer** (`poll/data/`)
- JPA entities (suffixed with `Entity`, e.g., `PollEntity`)
- Spring Data JPA repositories
- Mappers between domain and persistence models
- **MUST** implement repository interfaces from business layer
- **MUST NOT** expose JPA entities outside this layer
- **MAY** use Spring Data JPA, Hibernate features

### Domain-Driven Design Principles

**Entities vs Value Objects:**

**Entity** - Has unique identity, mutable lifecycle, needs to be found by ID
```java
// Example: Poll is an Entity
public class Poll {
    private final PollId id;  // Unique identity
    private PollTitle title;   // Can change over time
    private PollState state;   // Lifecycle transitions
}
```

**Value Object** - Immutable, no identity, compared by values
```java
// Example: PollTitle is a Value Object
public record PollTitle(String value) {
    public PollTitle {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
    }
}
```

**When to use each:**
- **Entity**: Poll (has lifecycle), User (has identity)
- **Value Object**: Option, Vote, PollTitle, Schedule (immutable data)

**Aggregates:**
- **Poll** is the aggregate root - controls all access to Options and Votes
- External code MUST access Options/Votes through Poll
- Maintain consistency boundaries within aggregates
- Repository interfaces exist only for aggregate roots

**Ubiquitous Language:**
- Use domain terms from `businessModel.md` in code
- Class/method names must match business terminology
- Avoid technical jargon in business layer (no "DTO", "Entity", "Manager")

**Example naming:**
```java
// ✅ GOOD - Domain language
public class CastVoteUseCase { ... }
public Poll findPollByTitle(PollTitle title) { ... }

// ❌ BAD - Technical language in business layer
public class VoteManager { ... }
public PollEntity getPollDTO(String titleString) { ... }
```

### Clean Code Standards

**Immutability:**
- Prefer immutable objects, especially value objects
- Use Java records for value objects when possible
- Use `final` fields in entities where appropriate

**Explicit over Implicit:**
```java
// ✅ GOOD - Explicit domain operation
poll.startVoting();

// ❌ BAD - Implicit through setter
poll.setState(PollState.ONGOING);
```

**Meaningful Names:**
```java
// ✅ GOOD
public class InitiatePollUseCase { ... }
public Vote castVote(Map<Option, Integer> rankings) { ... }

// ❌ BAD
public class PollService { ... }
public Vote process(Map<Object, Integer> data) { ... }
```

**Small Methods:**
- Methods should do ONE thing
- Aim for < 20 lines per method
- Extract complex logic into named private methods

**No Primitive Obsession:**
```java
// ✅ GOOD - Wrap primitives in value objects
public record PollId(UUID value) { }
public record PollTitle(String value) { }

// ❌ BAD - Raw primitives in domain
public void createPoll(UUID id, String title) { ... }
```

**Fail Fast:**
- Validate inputs immediately
- Throw domain exceptions with clear messages
- Use Objects.requireNonNull() for critical parameters

### Dependency Rules

**STRICT DEPENDENCY FLOW:**
```
facade → business → data
  ↓         ↓         ↓
HTTP     Domain    JPA
JSON     Logic     SQL
```

**NEVER:**
- Business layer MUST NOT import from facade or data
- Data layer MUST NOT import from facade
- No circular dependencies between packages

**ALLOWED:**
- Facade can import business (uses use cases)
- Business defines interfaces that data implements (dependency inversion)
- Data can import business interfaces only (not concrete classes)

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

## Code Organization & Development Guidelines

### Package Structure Convention

```
de.tonypsilon.rankify.api/
├── {context}/                    # Bounded context (e.g., poll, evaluation)
│   ├── business/                 # Domain layer - pure business logic
│   │   ├── {Aggregate}.java      # Aggregate root entity
│   │   ├── {ValueObject}.java    # Value objects (often records)
│   │   ├── {UseCase}UseCase.java # Use case interface
│   │   ├── {Exception}.java      # Domain exceptions
│   │   └── {Port}Repository.java # Repository interface (port)
│   ├── data/                     # Infrastructure layer - persistence
│   │   ├── {Entity}Entity.java   # JPA entities (suffix: Entity)
│   │   ├── Jpa{Entity}Repository.java
│   │   ├── SpringData{Entity}Repository.java
│   │   └── {Entity}Mapper.java   # Domain ↔ JPA mapping
│   └── facade/                   # Presentation layer - HTTP/REST
│       ├── {Operation}Controller.java
│       ├── {Operation}Command.java    # Input DTOs
│       └── jackson/              # JSON serialization
└── infrastructure/               # Cross-cutting concerns
    └── transaction/
```

**Naming Conventions:**

| Type | Pattern | Example |
|------|---------|---------|
| Aggregate Root | `{DomainConcept}` | `Poll`, `Vote` |
| Value Object | `{Concept}` or record | `PollTitle`, `Option` |
| Use Case | `{Verb}{Noun}UseCase` | `InitiatePollUseCase` |
| Controller | `{Operation}Controller` | `CastVoteController` |
| Command DTO | `{Operation}Command` | `CastVoteCommand` |
| Repository Interface | `{Entity}Repository` | `PollRepository` |
| JPA Repository | `Jpa{Entity}Repository` | `JpaPollRepository` |
| JPA Entity | `{Concept}Entity` | `PollEntity` |
| Mapper | `{Entity}Mapper` | `PollMapper` |

### Implementing New Features

**Step 1: Model the Domain (business layer)**
```java
// 1. Define value objects
public record NewConcept(String value) {
    public NewConcept {
        Objects.requireNonNull(value, "value cannot be null");
    }
}

// 2. Add to aggregate root
public class Poll {
    public void performDomainOperation(NewConcept concept) {
        // Business logic here
        // Validate invariants
        // Update state
    }
}

// 3. Create use case interface
public interface PerformOperationUseCase {
    void execute(PollId pollId, NewConcept concept);
}
```

**Step 2: Implement Persistence (data layer if needed)**
```java
// 1. Update JPA entity if needed
@Entity
@Table(name = "poll")
public class PollEntity {
    @Column(name = "new_concept")
    private String newConcept;
}

// 2. Update mapper
public class PollMapper {
    public Poll toDomain(PollEntity entity) {
        // Map new field
    }
}
```

**Step 3: Expose via API (facade layer)**
```java
// 1. Create command DTO
public record PerformOperationCommand(
    @NotBlank String concept
) {}

// 2. Create controller
@RestController
public class PerformOperationController {
    private final PerformOperationUseCase useCase;
    
    @PostMapping("/polls/{pollId}/operation")
    public ResponseEntity<Void> perform(
        @PathVariable UUID pollId,
        @RequestBody @Valid PerformOperationCommand command
    ) {
        useCase.execute(
            new PollId(pollId),
            new NewConcept(command.concept())
        );
        return ResponseEntity.ok().build();
    }
}
```

### Entity vs Value Object Guidelines

**Use Entity when:**
- Object has a unique identifier (ID)
- Identity matters more than attributes
- Object has a lifecycle with state changes
- Need to track object over time
- Object is the root of an aggregate

**Use Value Object when:**
- Object is defined by its attributes
- Two objects with same values are interchangeable
- Object is immutable
- No need for independent identity
- Part of a larger entity

**Implementation patterns:**

**Entity:**
```java
public class Poll {
    private final PollId id;           // Immutable identity
    private PollTitle title;            // Mutable state
    private PollState state;            // Lifecycle
    
    // Business methods that modify state
    public void updateTitle(PollTitle newTitle) {
        this.title = Objects.requireNonNull(newTitle);
    }
}
```

**Value Object (using record):**
```java
public record Option(
    String text,
    int displayOrder
) {
    public Option {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Option text cannot be blank");
        }
    }
    
    // No setters - immutable
    // Equality by value
}
```

**Value Object (using class when behavior needed):**
```java
public class Schedule {
    private final LocalDateTime start;
    private final LocalDateTime end;
    
    public Schedule(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
        validateSchedule();
    }
    
    // Return new instance instead of modifying
    public Schedule withStart(LocalDateTime newStart) {
        return new Schedule(newStart, this.end);
    }
    
    private void validateSchedule() {
        if (end != null && start != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
    }
}
```

### Use Case Implementation Pattern

**Use cases are the application's entry points to business logic.**

**Structure:**
```java
// 1. Define interface in business layer
public interface PerformBusinessOperationUseCase {
    ResultType execute(InputType input);
}

// 2. Implement in business layer
@Component  // Minimal Spring annotation
public class PerformBusinessOperation implements PerformBusinessOperationUseCase {
    
    private final PollRepository pollRepository;  // Port interface
    
    public PerformBusinessOperation(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }
    
    @Override
    @Transactional  // Transaction boundary at use case level
    public ResultType execute(InputType input) {
        // 1. Validate input
        Objects.requireNonNull(input);
        
        // 2. Load aggregate from repository
        Poll poll = pollRepository.findById(input.pollId())
            .orElseThrow(() -> new PollNotFoundException(input.pollId()));
        
        // 3. Execute domain operation
        poll.performDomainOperation(input.parameter());
        
        // 4. Save aggregate
        pollRepository.save(poll);
        
        // 5. Return result
        return createResult(poll);
    }
}
```

**Use Case Guidelines:**
- One use case = one business operation
- Keep use cases thin - delegate to domain model
- Use cases orchestrate, domain objects execute
- Transaction boundaries at use case level
- No business logic in use cases (belongs in domain model)

### Repository Pattern

**Interface (Port) in business layer:**
```java
public interface PollRepository {
    Optional<Poll> findById(PollId id);
    Poll save(Poll poll);
    void delete(PollId id);
    List<Poll> findByState(PollState state);
}
```

**Implementation (Adapter) in data layer:**
```java
@Component
public class JpaPollRepository implements PollRepository {
    
    private final SpringDataPollRepository springDataRepo;
    private final PollMapper mapper;
    
    @Override
    public Optional<Poll> findById(PollId id) {
        return springDataRepo.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public Poll save(Poll poll) {
        PollEntity entity = mapper.toEntity(poll);
        PollEntity saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }
}
```

**Repository Guidelines:**
- Only aggregate roots have repositories
- Repository interface in business layer (port)
- Implementation in data layer (adapter)
- Never expose JPA entities from repository
- Always map between domain and persistence models

### Testing Guidelines

**Test Layers Independently:**

**Business Layer Tests:**
```java
class PollTest {
    @Test
    void castVote_withValidRankings_createsVote() {
        // Given - Pure domain objects
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = Map.of(option1, 1, option2, 2);
        
        // When
        Vote vote = poll.castVote(rankings);
        
        // Then
        assertThat(vote.pollId()).isEqualTo(poll.id());
        assertThat(vote.rankings()).hasSize(2);
    }
}
```

**Use Case Tests (with mocks):**
```java
class CastVoteUseCaseTest {
    @Mock private PollRepository pollRepository;
    private CastVoteUseCase useCase;
    
    @Test
    void execute_withOngoingPoll_castsVote() {
        // Given
        Poll poll = createOngoingPoll();
        when(pollRepository.findById(any())).thenReturn(Optional.of(poll));
        
        // When
        useCase.execute(pollId, rankings);
        
        // Then
        verify(pollRepository).save(any(Poll.class));
    }
}
```

**Controller Integration Tests:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class CastVoteControllerTest {
    @Autowired private MockMvc mockMvc;
    
    @Test
    void castVote_withValidCommand_returns200() throws Exception {
        mockMvc.perform(post("/polls/{id}/votes", pollId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJsonCommand))
            .andExpect(status().isOk());
    }
}
```

## Quality Standards & Best Practices

### Code Review Checklist

**Architecture:**
- [ ] No business logic in controllers
- [ ] No infrastructure concerns in domain model
- [ ] Dependencies flow correctly (facade → business → data)
- [ ] Repository interfaces defined in business layer
- [ ] No circular dependencies

**Domain Model:**
- [ ] Entities have clear identity
- [ ] Value objects are immutable
- [ ] Business rules enforced in domain model
- [ ] Domain exceptions with meaningful names
- [ ] Ubiquitous language used consistently

**Clean Code:**
- [ ] Methods do one thing and do it well
- [ ] Names reveal intent
- [ ] No primitive obsession
- [ ] Fail-fast validation
- [ ] Minimal comments (code is self-documenting)

**Testing:**
- [ ] Unit tests for domain logic
- [ ] Use case tests with mocked repositories
- [ ] Integration tests for controllers
- [ ] Tests follow Arrange-Act-Assert pattern

### Common Anti-Patterns to Avoid

**❌ Anemic Domain Model:**
```java
// BAD - Just data holders, no behavior
public class Poll {
    private UUID id;
    private String title;
    // Only getters/setters
}

public class PollService {
    public void startPoll(Poll poll) {
        poll.setState("ONGOING");  // Business logic in service!
    }
}
```

**✅ Rich Domain Model:**
```java
// GOOD - Behavior in domain object
public class Poll {
    private final PollId id;
    private PollState state;
    
    public void startVoting() {
        if (state != PollState.IN_PREPARATION) {
            throw new IllegalStateException("Cannot start voting");
        }
        this.state = PollState.ONGOING;
    }
}
```

**❌ Transaction Script:**
```java
// BAD - Procedural logic in use case
public class CreatePollUseCase {
    public void execute(CreatePollCommand cmd) {
        if (cmd.title().isBlank()) throw new Exception();
        if (cmd.options().size() < 2) throw new Exception();
        // More validation...
        // More business rules...
        // Save to database...
    }
}
```

**✅ Domain Model Transaction:**
```java
// GOOD - Domain object enforces rules
public class CreatePollUseCase {
    public void execute(CreatePollCommand cmd) {
        Poll poll = Poll.create(  // Factory method with validation
            new PollTitle(cmd.title()),
            cmd.options().stream()
                .map(Option::new)
                .collect(toSet())
        );
        pollRepository.save(poll);
    }
}

public class Poll {
    public static Poll create(PollTitle title, Set<Option> options) {
        if (options.size() < 2) {
            throw new InsufficientOptionsException();
        }
        // Domain enforces its own rules
        return new Poll(...);
    }
}
```

**❌ Leaky Abstractions:**
```java
// BAD - JPA entities exposed to facade
@RestController
public class PollController {
    public ResponseEntity<PollEntity> getPoll(UUID id) {
        return pollRepository.findById(id);  // Leaking JPA!
    }
}
```

**✅ Proper Layer Isolation:**
```java
// GOOD - Domain model exposed, JPA hidden
@RestController
public class PollController {
    public ResponseEntity<Poll> getPoll(UUID id) {
        return ResponseEntity.ok(
            pollRepository.findById(new PollId(id))
                .orElseThrow(() -> new PollNotFoundException(id))
        );
    }
}
```

### When to Create New Abstractions

**Create new bounded context when:**
- Distinct business capability
- Different team ownership
- Independent release cycle
- Example: `evaluation` (result calculation) is separate from `poll` (voting)

**Create new value object when:**
- Wrapping primitive with validation
- Grouping related values
- Enforcing business rules
- Example: `PollTitle`, `Schedule`, `Option`

**Create new use case when:**
- New business operation
- New API endpoint needed
- Composing multiple domain operations
- Example: Each controller method typically maps to one use case

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

**Current Bounded Contexts:**
- **poll** - Core voting domain (aggregate root: Poll)
- **evaluation** - Result calculation and ranking algorithms
- **math** - Mathematical utilities for ranking calculations
- **infrastructure** - Cross-cutting concerns (transactions, etc.)

**Domain Model (poll context):**
- **Poll** (Entity/Aggregate Root) - Main aggregate with lifecycle states
- **Option** (Value Object) - Voting choices within polls
- **Vote** (Value Object) - User rankings of options
- **Ballot** (Value Object) - Presentation of options for voting
- **PollResult** (Value Object) - Calculated voting results

**Implementation Layers:**
- `poll/business/` - Domain model, use cases, repository ports
- `poll/data/` - JPA entities, Spring Data repositories, mappers
- `poll/facade/` - REST controllers, DTOs, JSON serialization

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

## Code Examples & Common Scenarios

### Adding a New Domain Concept

**Scenario: Add "PollDescription" value object**

```java
// 1. business/PollDescription.java - Value Object
public record PollDescription(String value) {
    private static final int MAX_LENGTH = 500;
    
    public PollDescription {
        if (value != null && value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Description cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}

// 2. Update Poll entity
public class Poll {
    private final PollId id;
    private PollTitle title;
    private PollDescription description;  // Add new field
    
    public void updateDescription(PollDescription newDescription) {
        this.description = Objects.requireNonNull(newDescription);
    }
}

// 3. data/PollEntity.java - Add JPA mapping
@Entity
public class PollEntity {
    @Column(name = "description", length = 500)
    private String description;
}

// 4. data/PollMapper.java - Update mapper
public Poll toDomain(PollEntity entity) {
    return new Poll(
        new PollId(entity.getId()),
        new PollTitle(entity.getTitle()),
        entity.getDescription() != null 
            ? new PollDescription(entity.getDescription())
            : null,
        // ... other fields
    );
}

// 5. facade/InitiatePollCommand.java - Add to DTO
public record InitiatePollCommand(
    @NotBlank String title,
    String description,  // Optional field
    @NotNull Set<String> options
) {}
```

### Adding a New Use Case

**Scenario: Add ability to archive finished polls**

```java
// 1. business/ArchivePollUseCase.java - Define interface
public interface ArchivePollUseCase {
    void execute(PollId pollId);
}

// 2. business/ArchivePoll.java - Implement use case
@Component
public class ArchivePoll implements ArchivePollUseCase {
    
    private final PollRepository pollRepository;
    
    public ArchivePoll(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }
    
    @Override
    @Transactional
    public void execute(PollId pollId) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new PollNotFoundException(pollId));
        
        poll.archive();  // Domain method enforces business rules
        
        pollRepository.save(poll);
    }
}

// 3. business/Poll.java - Add domain behavior
public class Poll {
    public void archive() {
        if (state() != PollState.FINISHED) {
            throw new IllegalStateException(
                "Can only archive finished polls"
            );
        }
        this.archived = true;
    }
}

// 4. facade/ArchivePollController.java - Expose via REST
@RestController
@RequestMapping("/polls")
public class ArchivePollController {
    
    private final ArchivePollUseCase archivePollUseCase;
    
    @PostMapping("/{pollId}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID pollId) {
        archivePollUseCase.execute(new PollId(pollId));
        return ResponseEntity.noContent().build();
    }
}
```

### Adding Business Validation

**Scenario: Validate minimum number of options when creating poll**

```java
// 1. Add validation in domain model
public class Poll {
    private static final int MINIMUM_OPTIONS = 2;
    
    public static Poll create(
        PollId id,
        PollTitle title,
        Set<Option> options
    ) {
        validateOptions(options);
        
        return new Poll(
            id, 
            title,
            new Ballot(options),
            Schedule.notStarted(),
            LocalDateTime.now()
        );
    }
    
    private static void validateOptions(Set<Option> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException(
                "Poll must have at least one option"
            );
        }
        if (options.size() < MINIMUM_OPTIONS) {
            throw new InsufficientOptionsException(
                "Poll must have at least " + MINIMUM_OPTIONS + " options"
            );
        }
    }
}

// 2. Create domain exception
public class InsufficientOptionsException extends RuntimeException {
    public InsufficientOptionsException(String message) {
        super(message);
    }
}

// 3. Handle in controller
@RestControllerAdvice
public class PollExceptionHandler {
    
    @ExceptionHandler(InsufficientOptionsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientOptions(
        InsufficientOptionsException ex
    ) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### Working with Aggregates

**Scenario: Ensure votes are always created through Poll aggregate**

```java
// ✅ CORRECT - Through aggregate root
public class Poll {
    public Vote castVote(Map<Option, Integer> rankings) {
        // Poll validates the vote
        if (state() != PollState.ONGOING) {
            throw new PollNotReadyForVotingException(id);
        }
        
        // Poll enforces completeness
        Map<Option, Integer> completedRankings = ensureAllOptionsRanked(rankings);
        
        return new RecordedVote(id, completedRankings);
    }
}

// Usage in use case
@Component
public class CastVote implements CastVoteUseCase {
    public void execute(PollId pollId, Map<Option, Integer> rankings) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new PollNotFoundException(pollId));
        
        Vote vote = poll.castVote(rankings);  // Through aggregate!
        
        voteRepository.save(vote);
    }
}

// ❌ WRONG - Bypassing aggregate root
@Component
public class CastVote implements CastVoteUseCase {
    public void execute(PollId pollId, Map<Option, Integer> rankings) {
        // Don't validate through Poll!
        Vote vote = new RecordedVote(pollId, rankings);
        voteRepository.save(vote);  // Business rules bypassed!
    }
}
```

### Maintaining Immutability

**Scenario: Update poll schedule while maintaining immutability**

```java
// Value Object - Schedule
public class Schedule {
    private final LocalDateTime start;
    private final LocalDateTime end;
    
    // Private constructor
    private Schedule(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }
    
    // Factory methods
    public static Schedule notStarted() {
        return new Schedule(null, null);
    }
    
    // Return NEW instance, don't modify
    public Schedule withStart(LocalDateTime start) {
        return new Schedule(start, this.end);
    }
    
    public Schedule withEnd(LocalDateTime end) {
        return new Schedule(this.start, end);
    }
    
    // No setters!
}

// Usage in Poll
public class Poll {
    private Schedule schedule;  // Mutable reference to immutable value
    
    public void startVoting() {
        // Replace entire schedule object
        this.schedule = this.schedule.withStart(LocalDateTime.now());
    }
}
```

## Final Notes

### Development Workflow

- Always start by understanding the domain model in `businessModel.md`
- Design domain model first, then persistence, then API
- Write unit tests for domain logic before implementation
- Use use cases to orchestrate, domain objects to execute
- Keep controllers thin - just translation layer
- Validate at domain boundaries, not in infrastructure

### When Making Changes

1. **Understand the domain** - Read business model documentation
2. **Identify the aggregate** - Which entity owns this operation?
3. **Model the behavior** - Add method to domain object, not service
4. **Implement the use case** - Orchestrate domain operations
5. **Expose via API** - Create controller and DTOs
6. **Test each layer** - Unit tests for domain, integration for API

### Code Review Focus Areas

- Is business logic in the domain model, not controllers?
- Are entities and value objects used correctly?
- Does the code speak the ubiquitous language?
- Are dependencies flowing correctly (facade → business → data)?
- Are domain objects properly tested?
- Is the solution the simplest that could work?

### Remember

- **Domain first**: Business rules belong in domain objects
- **Immutability**: Prefer immutable value objects
- **Aggregates**: Access children only through aggregate roots
- **Use cases**: One use case per business operation
- **Repositories**: Only for aggregate roots
- **Testing**: Test behavior, not implementation

### Additional Resources

- `businessModel.md` - Authoritative domain model specification
- `technicalDatamodel.md` - Database schema and persistence details
- `readme.md` - Setup and development environment

---

**When in doubt**: Favor explicit domain modeling over technical convenience. The domain should drive the design, not the database or framework.

### Environment & Build Notes

- Always run validation commands after making changes
- Use the health endpoint to verify application status before testing functionality
- Check both application and database logs when troubleshooting issues
- The containerized development environment is the canonical setup - local alternatives may have compatibility issues
- In production environments, full container orchestration works as designed despite sandbox limitations