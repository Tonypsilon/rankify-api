package de.tonypsilon.rankify.api.poll.facade;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests hitting real HTTP endpoints with an embedded server + PostgreSQL Testcontainer.
 * Covers poll creation, lifecycle state transitions (start/end voting) and input validation error scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PollIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("rankify_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private TestRestTemplate rest;

    // --- Happy path lifecycle -------------------------------------------------------------

    @Test
    void createStartEndPollLifecycle() {
        // 1. Create poll (no start/end set) => state implicitly IN_PREPARATION (start null, end null)
        CreatePollRequest createBody = new CreatePollRequest(
                new TitlePart("Lunch options"),
                new BallotPart(List.of(new OptionPart("Pizza"), new OptionPart("Sushi"))),
                new SchedulePart(null, null)
        );
        ResponseEntity<UUID> created = rest.postForEntity("/polls", createBody, UUID.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID pollId = created.getBody();
        assertThat(pollId).isNotNull();

        // 2. Fetch details right after creation
        PollDetailsResponse initialDetails = getDetails(pollId);
        assertThat(initialDetails.schedule().start()).isNull();
        assertThat(initialDetails.schedule().end()).isNull();
        assertThat(initialDetails.options()).hasSize(2);
        assertThat(initialDetails.options())
                .containsExactly("Pizza", "Sushi");
        assertThat(initialDetails.title()).isEqualTo("Lunch options");
        assertThat(initialDetails.state()).isEqualTo("IN_PREPARATION");

        // 3. Start voting
        patchPoll(pollId, new PatchRequest("START_VOTING", null, null, null), HttpStatus.NO_CONTENT);
        PollDetailsResponse afterStart = getDetails(pollId);
        assertThat(afterStart.schedule().start()).isNotNull();
        assertThat(afterStart.schedule().end()).isNull();
        assertThat(afterStart.state()).isEqualTo("ONGOING");

        // 4. End voting
        patchPoll(pollId, new PatchRequest("END_VOTING", null, null, null), HttpStatus.NO_CONTENT);
        PollDetailsResponse afterEnd = getDetails(pollId);
        assertThat(afterEnd.schedule().start()).isNotNull();
        assertThat(afterEnd.schedule().end()).isNotNull();
        assertThat(afterEnd.schedule().end()).isAfterOrEqualTo(afterEnd.schedule().start());
        assertThat(afterEnd.state()).isEqualTo("FINISHED");
    }

    // --- Validation / error scenarios -----------------------------------------------------
    @Nested
    class ValidationErrors {
        @Test
        void createPollWithSingleOptionReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Too few"), new BallotPart(List.of(new OptionPart("Only"))), new SchedulePart(null, null));
            ResponseEntity<String> resp = rest.postForEntity("/polls", body, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithBlankOptionReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Blank option"), new BallotPart(List.of(new OptionPart(" "), new OptionPart("B"))), new SchedulePart(null, null));
            ResponseEntity<String> resp = rest.postForEntity("/polls", body, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithEndBeforeStartReturnsBadRequest() {
            LocalDateTime now = LocalDateTime.now();
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Bad schedule"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(now, now.minusMinutes(5)));
            ResponseEntity<String> resp = rest.postForEntity("/polls", body, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithBlankTitleReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart(""), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(null, null));
            ResponseEntity<String> resp = rest.postForEntity("/polls", body, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithDuplicateOptionsReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Dupes"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("A"))), new SchedulePart(null, null));
            ResponseEntity<String> resp = rest.postForEntity("/polls", body, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchPollWithNullOperationReturnsBadRequest() {
            UUID pollId = createValidPoll();
            // Manually craft JSON with null operation to avoid serialization adjustments
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = "{\"operation\":null}"; // invalid
            ResponseEntity<String> resp = rest.exchange("/polls/" + pollId, HttpMethod.PATCH, new HttpEntity<>(json, headers), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateTitleWithoutNewTitleReturnsBadRequest() {
            UUID pollId = createValidPoll();
            ResponseEntity<String> resp = patchPoll(pollId, new PatchRequest("UPDATE_TITLE", null, null, null));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateScheduleWithoutNewScheduleReturnsBadRequest() {
            UUID pollId = createValidPoll();
            ResponseEntity<String> resp = patchPoll(pollId, new PatchRequest("UPDATE_SCHEDULE", null, null, null));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateOptionsWithoutNewBallotReturnsBadRequest() {
            UUID pollId = createValidPoll();
            ResponseEntity<String> resp = patchPoll(pollId, new PatchRequest("UPDATE_OPTIONS", null, null, null));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchPollStartVotingTwiceReturnsServerErrorIllegalState() {
            UUID pollId = createValidPoll();
            patchPoll(pollId, new PatchRequest("START_VOTING", null, null, null), HttpStatus.NO_CONTENT);
            ResponseEntity<String> resp = patchPoll(pollId, new PatchRequest("START_VOTING", null, null, null));
            assertThat(resp.getStatusCode().is5xxServerError()).isTrue();
        }
    }

    // --- Helpers --------------------------------------------------------------------------

    private PollDetailsResponse getDetails(UUID pollId) {
        ResponseEntity<PollDetailsResponse> details = rest.getForEntity("/polls/" + pollId, PollDetailsResponse.class);
        assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(details.getBody()).isNotNull();
        return details.getBody();
    }

    private UUID createValidPoll() {
        CreatePollRequest body = new CreatePollRequest(new TitlePart("Valid"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(null, null));
        ResponseEntity<UUID> resp = rest.postForEntity("/polls", body, UUID.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private void patchPoll(UUID pollId, PatchRequest body, HttpStatus expected) {
        ResponseEntity<Void> resp = rest.exchange("/polls/" + pollId, HttpMethod.PATCH, new HttpEntity<>(body, jsonHeaders()), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(expected);
    }

    private ResponseEntity<String> patchPoll(UUID pollId, PatchRequest body) {
        return rest.exchange("/polls/" + pollId, HttpMethod.PATCH, new HttpEntity<>(body, jsonHeaders()), String.class);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // --- Test-local request record types to craft valid & invalid JSON payloads ------------
    record CreatePollRequest(TitlePart title, BallotPart ballot, SchedulePart schedule) {}
    record TitlePart(String value) {}
    record BallotPart(List<OptionPart> options) {}
    record OptionPart(String text) {}
    record SchedulePart(LocalDateTime start, LocalDateTime end) {}

    // For PATCH requests; mirrors shape expected by PatchPollCommand (operation + optional fields)
    record PatchRequest(String operation, Map<String, Object> newTitle, Map<String, Object> newSchedule, Map<String, Object> newBallot) {}

    // --- Facade response mirror -----------------------------------------------------------
    record PollDetailsResponse(UUID id,
                               String title,
                               List<String> options,
                               ScheduleResponse schedule,
                               LocalDateTime created,
                               String state) {}
    record ScheduleResponse(LocalDateTime start, LocalDateTime end) {}
}
