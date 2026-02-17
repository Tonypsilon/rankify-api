package de.tonypsilon.rankify.api.poll.facade;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests hitting real HTTP endpoints with an embedded server + PostgreSQL Testcontainer.
 * Covers poll creation, lifecycle management (start/end voting) and input validation error scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PollIntegrationTest.LiquibaseTestConfiguration.class)
@Testcontainers
class PollIntegrationTest {

    @TestConfiguration
    static class LiquibaseTestConfiguration {
        @Bean
        public SpringLiquibase liquibase(DataSource dataSource) {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
            return liquibase;
        }
    }

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

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = restClientBuilder
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // --- Happy path lifecycle -------------------------------------------------------------

    @Test
    void createStartEndPollLifecycle() {
        // 1. Create poll (no start/end set) => poll is in preparation (start null, end null)
        CreatePollRequest createBody = new CreatePollRequest(
                new TitlePart("Lunch options"),
                new BallotPart(List.of(new OptionPart("Pizza"), new OptionPart("Sushi"))),
                new SchedulePart(null, null)
        );
        UUID pollId = restClient.post()
                .uri("/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBody)
                .retrieve()
                .onStatus(status -> status.value() != HttpStatus.CREATED.value(), 
                        (request, response) -> { throw new AssertionError("Expected 201 CREATED but got " + response.getStatusCode()); })
                .body(UUID.class);
        assertThat(pollId).isNotNull();

        // 2. Fetch details right after creation
        PollDetailsResponse initialDetails = getDetails(pollId);
        assertThat(initialDetails.schedule().start()).isNull();
        assertThat(initialDetails.schedule().end()).isNull();
        assertThat(initialDetails.options()).hasSize(2);
        assertThat(initialDetails.options())
                .containsExactly("Pizza", "Sushi");
        assertThat(initialDetails.title()).isEqualTo("Lunch options");

        // Fetch ballot via dedicated endpoint and verify options & order
        BallotResponse ballot = getBallot(pollId);
        assertThat(ballot).isNotNull();
        assertThat(ballot.options()).hasSize(2);
        assertThat(ballot.options().stream().map(OptionResponse::text).toList())
                .containsExactly("Pizza", "Sushi");

        // 3. Start voting
        patchPoll(pollId, new PatchRequest("START_VOTING", null, null, null), HttpStatus.NO_CONTENT);
        PollDetailsResponse afterStart = getDetails(pollId);
        assertThat(afterStart.schedule().start()).isNotNull();
        assertThat(afterStart.schedule().end()).isNull();

        // 3a. Cast votes while ongoing
        castVote(pollId, Map.of("Pizza", 1, "Sushi", 2));
        castVote(pollId, Map.of("Sushi", 1)); // partial ranking (Pizza will get sentinel)

        // 4. End voting
        patchPoll(pollId, new PatchRequest("END_VOTING", null, null, null), HttpStatus.NO_CONTENT);
        PollDetailsResponse afterEnd = getDetails(pollId);
        assertThat(afterEnd.schedule().start()).isNotNull();
        assertThat(afterEnd.schedule().end()).isNotNull();
        assertThat(afterEnd.schedule().end()).isAfterOrEqualTo(afterEnd.schedule().start());
    }

    // --- Validation / error scenarios -----------------------------------------------------
    @Nested
    class ValidationErrors {
        @Test
        void createPollWithSingleOptionReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Too few"), new BallotPart(List.of(new OptionPart("Only"))), new SchedulePart(null, null));
            HttpStatusCode status = restClient.post()
                    .uri("/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithBlankOptionReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Blank option"), new BallotPart(List.of(new OptionPart(" "), new OptionPart("B"))), new SchedulePart(null, null));
            HttpStatusCode status = restClient.post()
                    .uri("/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithEndBeforeStartReturnsBadRequest() {
            Instant now = Instant.now();
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Bad schedule"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(now, now.minusSeconds(300)));
            HttpStatusCode status = restClient.post()
                    .uri("/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithBlankTitleReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart(""), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(null, null));
            HttpStatusCode status = restClient.post()
                    .uri("/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createPollWithDuplicateOptionsReturnsBadRequest() {
            CreatePollRequest body = new CreatePollRequest(new TitlePart("Dupes"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("A"))), new SchedulePart(null, null));
            HttpStatusCode status = restClient.post()
                    .uri("/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchPollWithNullOperationReturnsBadRequest() {
            UUID pollId = createValidPoll();
            // Manually craft JSON with null operation to avoid serialization adjustments
            String json = "{\"operation\":null}"; // invalid
            HttpStatusCode status = restClient.patch()
                    .uri("/polls/" + pollId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity()
                    .getStatusCode();
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateTitleWithoutNewTitleReturnsBadRequest() {
            UUID pollId = createValidPoll();
            HttpStatusCode status = patchPollForStatus(pollId, new PatchRequest("UPDATE_TITLE", null, null, null));
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateScheduleWithoutNewScheduleReturnsBadRequest() {
            UUID pollId = createValidPoll();
            HttpStatusCode status = patchPollForStatus(pollId, new PatchRequest("UPDATE_SCHEDULE", null, null, null));
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchUpdateOptionsWithoutNewBallotReturnsBadRequest() {
            UUID pollId = createValidPoll();
            HttpStatusCode status = patchPollForStatus(pollId, new PatchRequest("UPDATE_OPTIONS", null, null, null));
            assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void patchPollStartVotingTwiceReturnsServerErrorIllegalState() {
            UUID pollId = createValidPoll();
            patchPoll(pollId, new PatchRequest("START_VOTING", null, null, null), HttpStatus.NO_CONTENT);
            HttpStatusCode status = patchPollForStatus(pollId, new PatchRequest("START_VOTING", null, null, null));
            assertThat(status.is5xxServerError()).isTrue();
        }
    }

    // --- Helpers --------------------------------------------------------------------------

    private PollDetailsResponse getDetails(UUID pollId) {
        PollDetailsResponse details = restClient.get()
                .uri("/polls/" + pollId)
                .retrieve()
                .body(PollDetailsResponse.class);
        assertThat(details).isNotNull();
        return details;
    }

    private BallotResponse getBallot(UUID pollId) {
        BallotResponse ballot = restClient.get()
                .uri("/polls/" + pollId + "/ballot")
                .retrieve()
                .body(BallotResponse.class);
        assertThat(ballot).isNotNull();
        return ballot;
    }

    private UUID createValidPoll() {
        CreatePollRequest body = new CreatePollRequest(new TitlePart("Valid"), new BallotPart(List.of(new OptionPart("A"), new OptionPart("B"))), new SchedulePart(null, null));
        UUID pollId = restClient.post()
                .uri("/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> status.value() != HttpStatus.CREATED.value(), 
                        (request, response) -> { throw new AssertionError("Expected 201 CREATED but got " + response.getStatusCode()); })
                .body(UUID.class);
        assertThat(pollId).isNotNull();
        return pollId;
    }

    private void patchPoll(UUID pollId, PatchRequest body, HttpStatus expected) {
        HttpStatusCode status = restClient.patch()
                .uri("/polls/" + pollId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toBodilessEntity()
                .getStatusCode();
        assertThat(status).isEqualTo(expected);
    }

    private HttpStatusCode patchPollForStatus(UUID pollId, PatchRequest body) {
        return restClient.patch()
                .uri("/polls/" + pollId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toBodilessEntity()
                .getStatusCode();
    }

    private void castVote(UUID pollId, Map<String, Integer> rankings) {
        CastVoteRequest body = new CastVoteRequest(rankings);
        HttpStatusCode status = restClient.post()
                .uri("/polls/" + pollId + "/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toBodilessEntity()
                .getStatusCode();
        assertThat(status).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- Test-local request record types to craft valid & invalid JSON payloads ------------
    record CreatePollRequest(TitlePart title, BallotPart ballot, SchedulePart schedule) {
    }

    record TitlePart(String value) {
    }

    record BallotPart(List<OptionPart> options) {
    }

    record OptionPart(String text) {
    }

    record SchedulePart(Instant start, Instant end) {
    }

    // For PATCH requests; mirrors shape expected by PatchPollCommand (operation + optional fields)
    record PatchRequest(String operation, Map<String, Object> newTitle, Map<String, Object> newSchedule,
                        Map<String, Object> newBallot) {
    }

    // --- Facade response mirror -----------------------------------------------------------
    record PollDetailsResponse(UUID id,
                               String title,
                               List<String> options,
                               ScheduleResponse schedule,
                               Instant created) {
    }

    record ScheduleResponse(Instant start, Instant end) {
    }

    // Added response mirrors for ballot endpoint
    record BallotResponse(List<OptionResponse> options) {
    }

    record OptionResponse(String text) {
    }

    // Local record mirroring request of CastVoteController
    record CastVoteRequest(Map<String, Integer> rankings) {
    }
}
