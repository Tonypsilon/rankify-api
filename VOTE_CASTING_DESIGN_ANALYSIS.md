# Vote Casting Design Analysis

## Current Implementation Analysis

### Overview
The current vote casting mechanism involves:
1. **Vote Interface** - Public interface with `pollId()` and `rankings()` methods
2. **RecordedVote** - Package-private record implementing Vote with validation logic
3. **Poll.castVote()** - Method on Poll entity that creates RecordedVote instances
4. **CastVoteUseCase** - Application service orchestrating the process

### Current Flow
```
CastVoteUseCase.castVote(pollId, rankings)
  → Poll poll = pollRepository.getById(pollId)
  → Vote vote = poll.castVote(rankings)
    → Validates poll state (ONGOING)
    → Validates options belong to ballot
    → Adds missing options with sentinel rank
    → Creates new RecordedVote(id, completedRankings)
  → pollRepository.saveVote(vote)
```

## Issues from DDD Perspective

### 1. Mixed Responsibilities in Poll Entity
**Problem**: Poll entity handles both:
- Its own lifecycle (state transitions, dates)
- Vote validation and creation logic

**DDD Principle Violated**: Single Responsibility Principle - An entity should focus on its own lifecycle and invariants.

**Impact**: 
- Poll is doing too much
- Vote creation logic is tightly coupled to Poll
- Harder to test vote creation independently

### 2. Factory Pattern Absence
**Problem**: No explicit factory for creating Vote instances. The Poll entity acts as an implicit factory.

**DDD Principle Violated**: Factory Pattern - Complex object creation should be delegated to factories, especially when validation is involved.

**Impact**:
- Creation logic is hidden inside Poll
- No clear separation between "what can be voted on" vs "how votes are created"
- Difficult to evolve vote creation logic independently

### 3. Package-Private Constructor with Validation
**Problem**: RecordedVote has a package-private constructor with validation logic. This creates:
- Tight coupling within the package
- Unclear boundaries of responsibility
- Validation happens in two places (Poll AND RecordedVote)

**DDD Principle Violated**: Information Hiding - Validation logic should be in one clear place.

**Impact**:
- Duplicate validation concerns
- Package-level coupling makes refactoring harder
- Unclear which component is responsible for what validation

### 4. Encapsulation Concerns
**Problem**: Poll exposes vote creation through `castVote()`, but this feels like it's exposing too much:
- Poll must know about all Option validation
- Poll must know about sentinel values for missing options
- Poll must know about RecordedVote construction

**DDD Principle**: Entities should delegate complex creation logic.

## Issues from Clean Architecture Perspective

### 1. Dependency Direction
**Current**: CastVoteUseCase → Poll → RecordedVote

**Issue**: The domain layer (Poll) is directly instantiating concrete implementations (RecordedVote), even though it returns an interface (Vote).

**Clean Architecture Principle**: Dependencies should point inward. The use case should depend on abstractions, not concrete implementations.

### 2. Business Logic Location
**Problem**: Critical business logic is split:
- Poll validates state and options
- RecordedVote validates rankings format
- No clear single source of truth

**Clean Architecture Principle**: Business rules should be centralized and easy to locate.

### 3. Testing Boundaries
**Problem**: 
- Can't test vote creation without involving Poll
- Can't test RecordedVote construction from outside the package
- Testing requires complex Poll setup even for simple vote validation

**Clean Architecture Principle**: Components should have clear boundaries that enable independent testing.

## Issues from Clean Code Perspective

### 1. Unclear Intent
**Problem**: The relationship between Vote interface and RecordedVote record is unclear:
- Why is RecordedVote package-private?
- Why does Poll create it?
- What's the role of the Vote interface?

**Clean Code Principle**: Code should communicate intent clearly.

### 2. Tell, Don't Ask
**Problem**: Poll must interrogate its ballot to validate options:
```java
if (!ballot().options().contains(option)) {
    throw new IllegalArgumentException(...);
}
```

**Clean Code Principle**: Objects should tell other objects what to do, not ask about their state.

### 3. Feature Envy
**Problem**: Poll's castVote() method is envious of Option and Ballot:
- It iterates through ranking options
- It checks ballot options
- It adds missing options
- This feels like it belongs elsewhere

**Clean Code Principle**: Methods should operate primarily on their own data.

## Proposed Solution: VoteFactory Pattern

### New Design

#### 1. Create VoteFactory (Domain Service)
```java
@Service // or could be a pure domain service without Spring annotation
public class VoteFactory {
    
    /**
     * Creates a vote for the given poll with the specified rankings.
     * Performs all validation and completes missing rankings.
     */
    public Vote createVote(Poll poll, Map<Option, Integer> rankings) {
        // Validate poll is ready for voting
        if (!poll.canAcceptVotes()) {
            throw new PollNotReadyForVotingException(poll.id());
        }
        
        // Validate and complete rankings
        Map<Option, Integer> completedRankings = validateAndCompleteRankings(
            poll.ballot(), 
            rankings
        );
        
        // Create the vote
        return new RecordedVote(poll.id(), completedRankings);
    }
    
    private Map<Option, Integer> validateAndCompleteRankings(
        Ballot ballot, 
        Map<Option, Integer> rankings
    ) {
        // Validation logic moved from Poll
        Map<Option, Integer> completed = new LinkedHashMap<>();
        
        // Validate supplied options belong to ballot
        for (Map.Entry<Option, Integer> entry : rankings.entrySet()) {
            Option option = entry.getKey();
            if (!ballot.options().contains(option)) {
                throw new IllegalArgumentException(
                    "Option " + option + " does not belong to this poll's ballot"
                );
            }
            completed.put(option, entry.getValue());
        }
        
        // Add missing options with sentinel rank
        for (Option ballotOption : ballot.options()) {
            completed.putIfAbsent(ballotOption, RecordedVote.MAX_RANKING);
        }
        
        return completed;
    }
}
```

#### 2. Simplify Poll Entity
```java
public class Poll {
    // ... existing fields and methods ...
    
    /**
     * Checks if this poll is ready to accept votes.
     */
    public boolean canAcceptVotes() {
        return state() == PollState.ONGOING;
    }
    
    // Remove castVote() method - vote creation is now VoteFactory's responsibility
}
```

#### 3. Update CastVoteUseCase
```java
@Service
public class CastVoteUseCase {
    private final PollRepository pollRepository;
    private final VoteFactory voteFactory;
    
    public CastVoteUseCase(PollRepository pollRepository, VoteFactory voteFactory) {
        this.pollRepository = pollRepository;
        this.voteFactory = voteFactory;
    }
    
    public void castVote(PollId pollId, Map<Option, Integer> rankings) {
        if (pollId == null) {
            throw new IllegalArgumentException("pollId must not be null");
        }
        Objects.requireNonNull(rankings, "rankings must not be null");
        
        Poll poll = pollRepository.getById(pollId);
        Vote vote = voteFactory.createVote(poll, rankings);
        pollRepository.saveVote(vote);
    }
}
```

#### 4. Keep RecordedVote Package-Private
RecordedVote remains package-private, but now it's only created by VoteFactory within the package. This maintains encapsulation while clarifying responsibility.

### Benefits of New Design

#### DDD Perspective
1. **Single Responsibility**: Poll focuses on its lifecycle, VoteFactory handles vote creation
2. **Factory Pattern**: Explicit factory for complex vote creation with validation
3. **Clear Boundaries**: Each component has a well-defined responsibility
4. **Domain Service**: VoteFactory is a domain service coordinating between Poll and Vote

#### Clean Architecture Perspective
1. **Proper Dependencies**: Use case depends on factory abstraction
2. **Business Logic Centralization**: All vote creation logic in one place (VoteFactory)
3. **Testability**: Can test vote creation independently of Poll setup
4. **Clear Boundaries**: Factory provides clear interface for vote creation

#### Clean Code Perspective
1. **Clear Intent**: VoteFactory's purpose is obvious from its name
2. **Tell, Don't Ask**: Factory tells RecordedVote to create itself with validated data
3. **No Feature Envy**: Each class operates on its own data
4. **Better Naming**: `canAcceptVotes()` is clearer than internal state checks

### Migration Path

1. Create VoteFactory in poll.business package
2. Move validation logic from Poll.castVote() to VoteFactory
3. Add Poll.canAcceptVotes() method
4. Update CastVoteUseCase to use VoteFactory
5. Keep existing Poll.castVote() temporarily as deprecated
6. Update all tests to use new pattern
7. Remove Poll.castVote() once all usages are migrated

### Alternative Considered: Making RecordedVote Public

**Option**: Make RecordedVote public and callable from outside the package.

**Rejected Because**:
- Violates encapsulation - RecordedVote is an implementation detail
- Would allow direct construction bypassing validation
- The Vote interface is the public contract; implementation should remain hidden
- Current package-private approach is correct; the factory provides the needed access point

## Conclusion

The current design is close but violates several DDD and Clean Code principles by:
1. Mixing Poll's responsibilities with vote creation
2. Lacking an explicit factory pattern
3. Splitting validation logic between Poll and RecordedVote

The proposed VoteFactory pattern addresses these issues by:
1. Creating a dedicated domain service for vote creation
2. Centralizing validation logic
3. Simplifying the Poll entity
4. Maintaining proper encapsulation with package-private RecordedVote
5. Improving testability and clarity

This refactoring better aligns with DDD principles, Clean Architecture, and Clean Code practices.
