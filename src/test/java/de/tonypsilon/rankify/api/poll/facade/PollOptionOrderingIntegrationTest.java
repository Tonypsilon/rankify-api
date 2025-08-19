package de.tonypsilon.rankify.api.poll.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the original ordering of options supplied during poll creation
 * is preserved when retrieving poll details and ballot. Uses an option sequence
 * that differs from natural/alphabetical ordering to detect unintended sorting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PollOptionOrderingIntegrationTest {

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

    record CreatePollRequest(TitlePart title, BallotPart ballot, SchedulePart schedule) {
    }

    record TitlePart(String value) {
    }

    record BallotPart(List<OptionPart> options) {
    }

    record OptionPart(String text) {
    }

    record SchedulePart(java.time.LocalDateTime start, java.time.LocalDateTime end) {
    }

    record PollDetailsResponse(UUID id, String title, List<String> options, ScheduleResponse schedule,
                               java.time.LocalDateTime created) {
    }

    record ScheduleResponse(java.time.LocalDateTime start, java.time.LocalDateTime end) {
    }

    record BallotResponse(List<OptionResponse> options) {
    }

    record OptionResponse(String text) {
    }

    @Test
    void createPollPreservesOptionOrder() {
        // Intentionally non-alphabetical order. Alphabetical would be Alpha, Bravo, Mike, Zulu.
        List<OptionPart> originalOrder = List.of(
                new OptionPart("Zulu"),
                new OptionPart("Alpha"),
                new OptionPart("Mike"),
                new OptionPart("Bravo")
        );
        CreatePollRequest body = new CreatePollRequest(new TitlePart("Ordering Test"), new BallotPart(originalOrder), new SchedulePart(null, null));

        ResponseEntity<UUID> created = rest.postForEntity("/polls", body, UUID.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID pollId = created.getBody();
        assertThat(pollId).isNotNull();

        // Fetch poll details and verify order preserved
        ResponseEntity<PollDetailsResponse> detailsResp = rest.getForEntity("/polls/" + pollId, PollDetailsResponse.class);
        assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PollDetailsResponse details = detailsResp.getBody();
        assertThat(details).isNotNull();
        assertThat(details.options()).containsExactly("Zulu", "Alpha", "Mike", "Bravo");

        // Fetch ballot endpoint and verify order preserved
        ResponseEntity<BallotResponse> ballotResp = rest.getForEntity("/polls/" + pollId + "/ballot", BallotResponse.class);
        assertThat(ballotResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BallotResponse ballot = ballotResp.getBody();
        assertThat(ballot).isNotNull();
        assertThat(ballot.options().stream().map(OptionResponse::text).toList())
                .containsExactly("Zulu", "Alpha", "Mike", "Bravo");
    }
}

