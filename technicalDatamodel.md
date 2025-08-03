# Technical Data Model - Rankify API

This document describes how the business data model defined in [businessModel.md](businessModel.md) is mapped to a relational database schema. The mapping follows JPA/Hibernate conventions and best practices for Spring Boot applications with PostgreSQL.

## Overview

The technical data model translates the domain-driven business model into a normalized relational database schema. The key principle is that **entities** get their own tables with primary keys, while **value objects** are either embedded within entity tables or stored in separate tables without independent identity.

## Database Technology Stack

- **Database**: PostgreSQL 12+
- **ORM**: Hibernate (via Spring Data JPA)
- **Connection Pooling**: HikariCP
- **Migration**: Flyway (recommended for schema versioning)

## Table Design and Mapping

### 1. Poll Table (Entity)

Since `Poll` is an entity with identity and lifecycle, it gets its own table.

```sql
CREATE TABLE polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    state VARCHAR(20) NOT NULL CHECK (state IN ('IN_PREPARATION', 'ONGOING', 'FINISHED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_polls_state ON polls(state);
CREATE INDEX idx_polls_dates ON polls(start_date, end_date);
```

**JPA Mapping:**
```java
@Entity
@Table(name = "polls")
public class Poll {
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
    
    @Enumerated(EnumType.STRING)
    private PollState state;
    
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Option> options = new HashSet<>();
}
```

### 2. Options Table (Value Object with Separate Table)

Although `Option` is a value object, it's stored in a separate table for normalization and to handle the one-to-many relationship with Poll efficiently.

```sql
CREATE TABLE options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    text VARCHAR(500) NOT NULL,
    display_order INTEGER,
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    UNIQUE(poll_id, text)  -- Ensure uniqueness within a poll
);

CREATE INDEX idx_options_poll_id ON options(poll_id);
CREATE INDEX idx_options_display_order ON options(poll_id, display_order);
```

**JPA Mapping:**
```java
@Entity
@Table(name = "options", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "text"}))
public class Option {
    @Id
    @GeneratedValue
    private UUID id;  -- Technical ID for JPA, not business identity
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;
    
    @Column(nullable = false, length = 500)
    private String text;
    
    @Column(name = "display_order")
    private Integer displayOrder;
}
```

### 3. Votes Table (Value Object with Separate Table)

`Vote` is a value object but requires its own table due to the complex ranking structure.

```sql
CREATE TABLE votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);

CREATE INDEX idx_votes_poll_id ON votes(poll_id);
CREATE INDEX idx_votes_submitted_at ON votes(submitted_at);
```

### 4. Vote Rankings Table (Component of Vote Value Object)

The rankings within a vote are stored in a separate table to handle the many-to-many relationship between votes and options with ranking data.

```sql
CREATE TABLE vote_rankings (
    vote_id UUID NOT NULL,
    option_id UUID NOT NULL,
    rank_value INTEGER NOT NULL CHECK (rank_value > 0),
    PRIMARY KEY (vote_id, option_id),
    FOREIGN KEY (vote_id) REFERENCES votes(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE
);

CREATE INDEX idx_vote_rankings_vote_id ON vote_rankings(vote_id);
CREATE INDEX idx_vote_rankings_option_id ON vote_rankings(option_id);
```

**JPA Mapping for Vote:**
```java
@Entity
@Table(name = "votes")
public class Vote {
    @Id
    @GeneratedValue
    private UUID id;  -- Technical ID for persistence
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VoteRanking> rankings = new HashSet<>();
}

@Entity
@Table(name = "vote_rankings")
public class VoteRanking {
    @EmbeddedId
    private VoteRankingId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("voteId")
    @JoinColumn(name = "vote_id")
    private Vote vote;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("optionId")
    @JoinColumn(name = "option_id")
    private Option option;
    
    @Column(name = "rank_value", nullable = false)
    private Integer rankValue;
}

@Embeddable
public class VoteRankingId {
    private UUID voteId;
    private UUID optionId;
}
```

### 5. Poll Results Table (Value Object - Calculated Data)

`PollResult` is stored in a separate table as it contains calculated, derived data that needs to be persisted for performance and audit purposes.

```sql
CREATE TABLE poll_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL UNIQUE,  -- One result per poll
    total_votes INTEGER NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_poll_results_poll_id ON poll_results(poll_id);
```

### 6. Ranked Options Table (Component of PollResult Value Object)

Final rankings for each option in the poll results.

```sql
CREATE TABLE ranked_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_result_id UUID NOT NULL,
    option_id UUID NOT NULL,
    final_rank INTEGER NOT NULL,
    first_place_votes INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (poll_result_id) REFERENCES poll_results(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE,
    UNIQUE(poll_result_id, option_id)
);

CREATE INDEX idx_ranked_options_poll_result ON ranked_options(poll_result_id);
CREATE INDEX idx_ranked_options_final_rank ON ranked_options(poll_result_id, final_rank);
```

**JPA Mapping for PollResult:**
```java
@Entity
@Table(name = "poll_results")
public class PollResult {
    @Id
    @GeneratedValue
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false, unique = true)
    private Poll poll;
    
    @Column(name = "total_votes", nullable = false)
    private Integer totalVotes;
    
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    
    @OneToMany(mappedBy = "pollResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("finalRank ASC")
    private List<RankedOption> finalRanking = new ArrayList<>();
}

@Entity
@Table(name = "ranked_options")
public class RankedOption {
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_result_id", nullable = false)
    private PollResult pollResult;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private Option option;
    
    @Column(name = "final_rank", nullable = false)
    private Integer finalRank;
    
    @Column(name = "first_place_votes", nullable = false)
    private Integer firstPlaceVotes;
}
```

## Value Object Handling Strategy

### Embedded vs Separate Tables

**Separate Tables (Chosen Approach):**
- **Options**: Separate table for better normalization and query performance
- **Votes & Rankings**: Separate tables due to complex many-to-many relationships
- **Poll Results**: Separate table for calculated data persistence
- **Ranked Options**: Separate table as component of PollResult

**Why not Embedded:**
- Complex relationships (many-to-many, collections)
- Need for independent querying
- Better normalization and referential integrity
- Performance considerations for large datasets

### Business Identity vs Technical Identity

**Value Objects with Technical IDs:**
While value objects don't have business identity, they get technical UUIDs for:
- JPA entity requirements
- Foreign key relationships
- Database performance (clustering, indexing)
- Referential integrity constraints

The technical ID is purely for persistence - business equality is still based on content.

## Database Schema Diagram

```mermaid
erDiagram
    polls {
        UUID id PK
        VARCHAR title
        TEXT description
        TIMESTAMP start_date
        TIMESTAMP end_date
        VARCHAR state
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    
    options {
        UUID id PK
        UUID poll_id FK
        VARCHAR text
        INTEGER display_order
    }
    
    votes {
        UUID id PK
        UUID poll_id FK
        TIMESTAMP submitted_at
    }
    
    vote_rankings {
        UUID vote_id FK,PK
        UUID option_id FK,PK
        INTEGER rank_value
    }
    
    poll_results {
        UUID id PK
        UUID poll_id FK,UNIQUE
        INTEGER total_votes
        TIMESTAMP calculated_at
    }
    
    ranked_options {
        UUID id PK
        UUID poll_result_id FK
        UUID option_id FK
        INTEGER final_rank
        INTEGER first_place_votes
    }
    
    polls ||--o{ options : "contains"
    polls ||--o{ votes : "receives"
    polls ||--|| poll_results : "produces"
    options ||--o{ vote_rankings : "ranked_in"
    votes ||--o{ vote_rankings : "contains"
    poll_results ||--o{ ranked_options : "includes"
    options ||--o{ ranked_options : "represented_in"
```

## Data Integrity and Constraints

### Primary Keys
- All tables use UUID primary keys for better distribution and security
- UUIDs are generated by the database (`gen_random_uuid()`) for consistency

### Foreign Key Constraints
```sql
-- Poll-Option relationship
ALTER TABLE options ADD CONSTRAINT fk_options_poll 
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE;

-- Poll-Vote relationship  
ALTER TABLE votes ADD CONSTRAINT fk_votes_poll 
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE;

-- Vote-VoteRanking relationship
ALTER TABLE vote_rankings ADD CONSTRAINT fk_vote_rankings_vote 
    FOREIGN KEY (vote_id) REFERENCES votes(id) ON DELETE CASCADE;

-- Option-VoteRanking relationship
ALTER TABLE vote_rankings ADD CONSTRAINT fk_vote_rankings_option 
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE;

-- Poll-PollResult relationship
ALTER TABLE poll_results ADD CONSTRAINT fk_poll_results_poll 
    FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE;

-- PollResult-RankedOption relationship
ALTER TABLE ranked_options ADD CONSTRAINT fk_ranked_options_result 
    FOREIGN KEY (poll_result_id) REFERENCES poll_results(id) ON DELETE CASCADE;

-- Option-RankedOption relationship  
ALTER TABLE ranked_options ADD CONSTRAINT fk_ranked_options_option 
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE;
```

### Unique Constraints
```sql
-- Unique option text within a poll
ALTER TABLE options ADD CONSTRAINT uk_options_poll_text 
    UNIQUE (poll_id, text);

-- One result per poll
ALTER TABLE poll_results ADD CONSTRAINT uk_poll_results_poll 
    UNIQUE (poll_id);

-- One ranking per option per vote
ALTER TABLE vote_rankings ADD CONSTRAINT pk_vote_rankings 
    PRIMARY KEY (vote_id, option_id);

-- One ranked option per poll result per option
ALTER TABLE ranked_options ADD CONSTRAINT uk_ranked_options_result_option 
    UNIQUE (poll_result_id, option_id);
```

### Check Constraints
```sql
-- Valid poll states
ALTER TABLE polls ADD CONSTRAINT ck_polls_state 
    CHECK (state IN ('IN_PREPARATION', 'ONGOING', 'FINISHED'));

-- Valid date range
ALTER TABLE polls ADD CONSTRAINT ck_polls_date_range 
    CHECK (start_date < end_date);

-- Positive rank values
ALTER TABLE vote_rankings ADD CONSTRAINT ck_vote_rankings_rank 
    CHECK (rank_value > 0);

-- Non-negative vote counts
ALTER TABLE poll_results ADD CONSTRAINT ck_poll_results_votes 
    CHECK (total_votes >= 0);

ALTER TABLE ranked_options ADD CONSTRAINT ck_ranked_options_votes 
    CHECK (first_place_votes >= 0);

-- Positive final ranks
ALTER TABLE ranked_options ADD CONSTRAINT ck_ranked_options_rank 
    CHECK (final_rank > 0);
```

## Indexing Strategy

### Performance Indexes
```sql
-- Poll lookups by state and date
CREATE INDEX idx_polls_state ON polls(state);
CREATE INDEX idx_polls_active ON polls(start_date, end_date) 
    WHERE state = 'ONGOING';

-- Option lookups
CREATE INDEX idx_options_poll_id ON options(poll_id);
CREATE INDEX idx_options_poll_display ON options(poll_id, display_order);

-- Vote lookups
CREATE INDEX idx_votes_poll_id ON votes(poll_id);
CREATE INDEX idx_votes_submitted_at ON votes(submitted_at);

-- Vote ranking queries
CREATE INDEX idx_vote_rankings_vote_id ON vote_rankings(vote_id);
CREATE INDEX idx_vote_rankings_option_id ON vote_rankings(option_id);

-- Result lookups
CREATE INDEX idx_ranked_options_poll_result ON ranked_options(poll_result_id);
CREATE INDEX idx_ranked_options_rank ON ranked_options(poll_result_id, final_rank);
```

## Query Patterns and Performance

### Common Query Examples

**Find active polls:**
```sql
SELECT * FROM polls 
WHERE state = 'ONGOING' 
  AND start_date <= CURRENT_TIMESTAMP 
  AND end_date > CURRENT_TIMESTAMP;
```

**Get poll with options:**
```sql
SELECT p.*, o.text, o.display_order 
FROM polls p 
LEFT JOIN options o ON p.id = o.poll_id 
WHERE p.id = ?
ORDER BY o.display_order;
```

**Count votes for a poll:**
```sql
SELECT COUNT(*) FROM votes WHERE poll_id = ?;
```

**Get vote rankings:**
```sql
SELECT v.submitted_at, o.text, vr.rank_value
FROM votes v
JOIN vote_rankings vr ON v.id = vr.vote_id
JOIN options o ON vr.option_id = o.id
WHERE v.poll_id = ?
ORDER BY v.submitted_at, vr.rank_value;
```

**Get poll results:**
```sql
SELECT ro.final_rank, o.text, ro.first_place_votes
FROM poll_results pr
JOIN ranked_options ro ON pr.id = ro.poll_result_id
JOIN options o ON ro.option_id = o.id
WHERE pr.poll_id = ?
ORDER BY ro.final_rank;
```

## Transaction Boundaries

### Entity Boundaries
- **Poll Aggregate**: Poll + Options (managed together)
- **Vote Aggregate**: Vote + VoteRankings (atomic unit)
- **Result Aggregate**: PollResult + RankedOptions (calculated together)

### Transaction Scope
```java
@Transactional
public void createPoll(PollCreationData data) {
    // Poll and Options created atomically
}

@Transactional
public void castVote(VoteData voteData) {
    // Vote and all rankings created atomically
}

@Transactional
public PollResult calculateResults(UUID pollId) {
    // Result calculation and persistence atomic
}
```

## Migration Strategy

### Schema Versioning with Flyway

**V1__Initial_schema.sql:**
```sql
-- Create all base tables
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE polls (
    -- Poll table definition
);

CREATE TABLE options (
    -- Options table definition  
);

-- etc.
```

**V2__Add_indexes.sql:**
```sql
-- Add performance indexes
CREATE INDEX idx_polls_state ON polls(state);
-- etc.
```

### Data Migration Considerations
- **Backwards Compatibility**: New columns should be nullable initially
- **Default Values**: Use database defaults for new required fields
- **Batch Processing**: Large data migrations should be batched
- **Rollback Strategy**: Always plan for schema rollbacks

## Performance Considerations

### Connection Pool Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
```

### JPA Configuration
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema management
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
```

### Query Optimization
- **Lazy Loading**: Use `@OneToMany(fetch = FetchType.LAZY)` by default
- **Batch Fetching**: Use `@BatchSize` for collections
- **Query Projections**: Use DTOs for read-only queries
- **Database-level Pagination**: Use `LIMIT`/`OFFSET` for large result sets

## Security Considerations

### Data Protection
- **UUIDs**: Prevent enumeration attacks
- **No PII**: Anonymous voting - no personal data stored
- **Audit Trail**: Track creation/modification timestamps
- **Soft Deletes**: Consider soft deletes for audit requirements

### Access Control
- **Application Level**: Security handled by Spring Security
- **Database Level**: Use dedicated application database users
- **Connection Security**: SSL/TLS for database connections

## Monitoring and Observability

### Database Metrics
- Connection pool utilization
- Query execution times
- Index usage statistics
- Lock contention monitoring

### Application Metrics
- Repository method execution times
- Transaction success/failure rates
- Entity cache hit rates
- Query result set sizes

This technical data model provides a robust foundation for implementing the business model in a relational database while maintaining data integrity, performance, and scalability.