# Manual System Test with cURL

This document provides comprehensive manual system tests for the Rankify API using cURL commands. These tests validate
the core poll management functionality and error handling behavior.

## Prerequisites

Before running these tests, ensure that:

1. The Rankify API application is running locally on `http://localhost:8080`
2. The PostgreSQL database is running and accessible
3. The application health check returns `{"status":"UP"}`:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Environment Setup

Start the application using the provided setup script:

```bash
./setup.sh
```

Wait for the services to be ready (usually 30-60 seconds), then verify:

```bash
curl http://localhost:8080/actuator/health
```

## Test Scenarios

### Happy Path: Complete Poll Lifecycle

This section tests the primary use case of creating a poll, starting voting, and ending voting while checking poll
details at each step.

#### Step 1: Create a New Poll

**Purpose:** Create a poll with two voting options.

**Command:**

```bash
curl -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{
    "title": {
      "value": "Lunch Options"
    },
    "ballot": {
      "options": [
        {"text": "Pizza"},
        {"text": "Sushi"}
      ]
    },
    "schedule": {
      "start": null,
      "end": null
    }
  }'
```

**Expected Response:**

- Status Code: `201 Created`
- Body: A UUID string (e.g., `"550e8400-e29b-41d4-a716-446655440000"`)

**What this does:**

- Creates a new poll with the title "Lunch Options"
- Adds two voting options: "Pizza" and "Sushi"
- Sets no specific start/end times (null values), leaving the poll ready for voting
- Returns the unique identifier for the created poll

**Save the returned UUID for subsequent commands (replace `{POLL_ID}` below with the actual UUID).**

#### Step 2: Get Initial Poll Details

**Purpose:** Verify the poll was created correctly.

**Command:**

```bash
curl -X GET http://localhost:8080/polls/{POLL_ID} \
  -H "Accept: application/json"
```

**Expected Response:**

- Status Code: `200 OK`
- Body structure:

```json
{
  "id": "{POLL_ID}",
  "title": "Lunch Options",
  "options": [
    "Pizza",
    "Sushi"
  ],
  "schedule": {
    "start": null,
    "end": null
  },
  "created": "2024-01-15T10:30:00.123456"
}
```

**What this verifies:**

- Poll exists and is retrievable
- Title and options are correctly stored
- Schedule has null start/end times
- Created timestamp is populated

#### Step 3: Start Voting

**Purpose:** Start voting for the poll.

**Command:**

```bash
curl -X PATCH http://localhost:8080/polls/{POLL_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "START_VOTING"
  }'
```

**Expected Response:**

- Status Code: `204 No Content`
- Body: Empty

**What this does:**

- Sets the schedule.start time to the current timestamp
- Allows voting to begin on this poll

#### Step 3a: Cast Votes While Ongoing

**Purpose:** Submit ranked votes for the poll while it is in the ongoing state.

**Command (full ranking):**

```bash
curl -X POST http://localhost:8080/polls/{POLL_ID}/votes \
  -H "Content-Type: application/json" \
  -d '{
    "rankings": {"Pizza": 1, "Sushi": 2}
  }'
```

**Expected Response:**

- Status Code: `204 No Content`

**Command (partial ranking – omitted options get lowest/sentinel rank internally):**

```bash
curl -X POST http://localhost:8080/polls/{POLL_ID}/votes \
  -H "Content-Type: application/json" \
  -d '{
    "rankings": {"Sushi": 1}
  }'
```

**Expected Response:**

- Status Code: `204 No Content`

**What this does:**

- Records two separate votes
- First voter prefers Pizza over Sushi
- Second voter only ranks Sushi; Pizza is auto-assigned an implicit lowest priority rank
- Votes are persisted (in the `votes` and `rankings` tables)

**Verification (optional DB check):**

```bash
docker compose exec database psql -U rankify -d rankify -c "SELECT count(*) FROM votes;"
docker compose exec database psql -U rankify -d rankify -c "SELECT * FROM rankings ORDER BY vote_id, rank;"
```

You can cast multiple additional votes to simulate more traffic.

#### Step 4: Verify Poll is Ongoing

**Purpose:** Confirm voting was started and start time was set.

**Command:**

```bash
curl -X GET http://localhost:8080/polls/{POLL_ID} \
  -H "Accept: application/json"
```

**Expected Response:**

- Status Code: `200 OK`
- Body changes from Step 2:

```json
{
  "id": "{POLL_ID}",
  "title": "Lunch Options",
  "options": [
    "Pizza",
    "Sushi"
  ],
  "schedule": {
    "start": "2024-01-15T10:35:00.456789",
    "end": null
  },
  "created": "2024-01-15T10:30:00.123456"
}
```

**What this verifies:**

- Start time is now populated with current timestamp
- End time remains null
- Poll is now accepting votes
- All other fields unchanged

#### Step 5: End Voting

**Purpose:** End voting for the poll.

**Command:**

```bash
curl -X PATCH http://localhost:8080/polls/{POLL_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "END_VOTING"
  }'
```

**Expected Response:**

- Status Code: `204 No Content`
- Body: Empty

**What this does:**

- Sets the schedule.end time to the current timestamp
- Closes voting on this poll

#### Step 6: Verify Poll is Finished

**Purpose:** Confirm the poll completed the full lifecycle successfully.

**Command:**

```bash
curl -X GET http://localhost:8080/polls/{POLL_ID} \
  -H "Accept: application/json"
```

**Expected Response:**

- Status Code: `200 OK`
- Body changes from Step 4:

```json
{
  "id": "{POLL_ID}",
  "title": "Lunch Options",
  "options": [
    "Pizza",
    "Sushi"
  ],
  "schedule": {
    "start": "2024-01-15T10:35:00.456789",
    "end": "2024-01-15T10:40:00.789012"
  },
  "created": "2024-01-15T10:30:00.123456"
}
```

**What this verifies:**

- End time is now populated with current timestamp
- Poll is no longer accepting new votes
- Start time remains from when voting started
- End time is after or equal to start time
- Poll has completed the full lifecycle: IN_PREPARATION → ONGOING → FINISHED

---

### Error Scenarios

These tests validate that the API properly handles invalid requests and returns appropriate error responses.

#### Error Test 1: Create Poll with Single Option

**Purpose:** Verify that polls requiring at least two options are enforced.

**Command:**

```bash
curl -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{
    "title": {
      "value": "Invalid Poll"
    },
    "ballot": {
      "options": [
        {"text": "Only One Option"}
      ]
    },
    "schedule": {
      "start": null,
      "end": null
    }
  }'
```

**Expected Response:**

- Status Code: `400 Bad Request`
- Body: Error message indicating ballot must have at least two options

**What this tests:** Business rule validation that polls must have multiple options for meaningful voting.

#### Error Test 2: Create Poll with Blank Title

**Purpose:** Verify that poll titles cannot be empty or blank.

**Command:**

```bash
curl -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{
    "title": {
      "value": ""
    },
    "ballot": {
      "options": [
        {"text": "Option A"},
        {"text": "Option B"}
      ]
    },
    "schedule": {
      "start": null,
      "end": null
    }
  }'
```

**Expected Response:**

- Status Code: `400 Bad Request`
- Body: Error message indicating title cannot be blank

**What this tests:** Input validation for required poll title field.

#### Error Test 3: Create Poll with Duplicate Options

**Purpose:** Verify that poll options must be unique.

**Command:**

```bash
curl -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{
    "title": {
      "value": "Duplicate Options Test"
    },
    "ballot": {
      "options": [
        {"text": "Same Option"},
        {"text": "Same Option"}
      ]
    },
    "schedule": {
      "start": null,
      "end": null
    }
  }'
```

**Expected Response:**

- Status Code: `400 Bad Request`
- Body: Error message indicating duplicate options are not allowed

**What this tests:** Business rule that poll options must be unique for clear voting choices.

#### Error Test 4: Patch with Invalid Operation

**Purpose:** Verify that patch operations must be valid enum values.

**Command:**

```bash
curl -X PATCH http://localhost:8080/polls/{POLL_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "operation": null
  }'
```

**Expected Response:**

- Status Code: `400 Bad Request`
- Body: Error message indicating operation cannot be null

**What this tests:** Input validation for required operation field in patch requests.

#### Error Test 5: Invalid State Transition

**Purpose:** Verify that polls cannot be started twice (business rule enforcement).

First, create and start a poll:

```bash
# Create poll
POLL_ID=$(curl -s -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{
    "title": {"value": "State Test"},
    "ballot": {"options": [{"text": "A"}, {"text": "B"}]},
    "schedule": {"start": null, "end": null}
  }')

# Start voting
curl -X PATCH http://localhost:8080/polls/$POLL_ID \
  -H "Content-Type: application/json" \
  -d '{"operation": "START_VOTING"}'
```

Then attempt to start voting again:

```bash
curl -X PATCH http://localhost:8080/polls/$POLL_ID \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "START_VOTING"
  }'
```

**Expected Response:**

- Status Code: `400 Bad Request` or `409 Conflict`
- Body: Error message indicating invalid operation

**What this tests:** Business logic that prevents invalid poll operations.

#### Error Test 6: Get Non-Existent Poll

**Purpose:** Verify proper handling of requests for polls that don't exist.

**Command:**

```bash
curl -X GET http://localhost:8080/polls/00000000-0000-0000-0000-000000000000 \
  -H "Accept: application/json"
```

**Expected Response:**

- Status Code: `404 Not Found`
- Body: Error message indicating poll not found

**What this tests:** Error handling for resource not found scenarios.

---

## Test Execution Tips

### Running the Complete Test Suite

1. **Start fresh:** Restart the application to ensure clean state
2. **Run happy path first:** Execute Steps 1-6 in sequence with a new poll
3. **Save poll IDs:** Keep track of UUIDs returned from poll creation
4. **Run error tests:** Execute each error scenario independently
5. **Verify responses:** Check both status codes and response bodies

### Expected Timing

- Poll creation: < 1 second
- State transitions: < 1 second
- Poll retrieval: < 500ms
- Error responses: < 500ms

### Troubleshooting

- **Connection refused:** Ensure application is running on port 8080
- **Database errors:** Verify PostgreSQL container is healthy
- **JSON parsing errors:** Check request body formatting and Content-Type headers
- **404 errors:** Verify you're using correct poll UUIDs from creation responses

### Automation Helper

To run the complete happy path test automatically:

```bash
#!/bin/bash
# Create poll and capture ID
POLL_ID=$(curl -s -X POST http://localhost:8080/polls \
  -H "Content-Type: application/json" \
  -d '{"title":{"value":"Automated Test"},"ballot":{"options":[{"text":"A"},{"text":"B"}]},"schedule":{"start":null,"end":null}}' | tr -d '"')

echo "Created poll: $POLL_ID"

# Get initial details
curl -s -X GET http://localhost:8080/polls/$POLL_ID | jq .

# Start voting
curl -s -X PATCH http://localhost:8080/polls/$POLL_ID \
  -H "Content-Type: application/json" \
  -d '{"operation":"START_VOTING"}'

echo "Started voting"

# Get updated details
curl -s -X GET http://localhost:8080/polls/$POLL_ID | jq .

# End voting  
curl -s -X PATCH http://localhost:8080/polls/$POLL_ID \
  -H "Content-Type: application/json" \
  -d '{"operation":"END_VOTING"}'

echo "Ended voting"

# Get final details
curl -s -X GET http://localhost:8080/polls/$POLL_ID | jq .
```

This completes the manual system test documentation for the Rankify API.