package de.tonypsilon.rankify.api.poll.facade;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the original ordering of options supplied during poll creation
 * is preserved when retrieving poll details and ballot. Uses an option sequence
 * that differs from natural/alphabetical ordering to detect unintended sorting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PollOptionOrderingIntegrationTest.LiquibaseTestConfiguration.class)
@Testcontainers
class PollOptionOrderingIntegrationTest {

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

    record CreatePollRequest(TitlePart title, BallotPart ballot, SchedulePart schedule) {
    }

    record TitlePart(String value) {
    }

    record BallotPart(List<OptionPart> options) {
    }

    record OptionPart(String text) {
    }

    record SchedulePart(java.time.Instant start, java.time.Instant end) {
    }

    record PollDetailsResponse(UUID id, String title, List<String> options, ScheduleResponse schedule,
                               java.time.Instant created) {
    }

    record ScheduleResponse(java.time.Instant start, java.time.Instant end) {
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

        UUID pollId = restClient.post()
                .uri("/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> status.value() != HttpStatus.CREATED.value(),
                        (request, response) -> { throw new AssertionError("Expected 201 CREATED but got " + response.getStatusCode()); })
                .body(UUID.class);
        assertThat(pollId).isNotNull();

        // Fetch poll details and verify order preserved
        PollDetailsResponse details = restClient.get()
                .uri("/polls/" + pollId)
                .retrieve()
                .body(PollDetailsResponse.class);
        assertThat(details).isNotNull();
        assertThat(details.options()).containsExactly("Zulu", "Alpha", "Mike", "Bravo");

        // Fetch ballot endpoint and verify order preserved
        BallotResponse ballot = restClient.get()
                .uri("/polls/" + pollId + "/ballot")
                .retrieve()
                .body(BallotResponse.class);
        assertThat(ballot).isNotNull();
        assertThat(ballot.options().stream().map(OptionResponse::text).toList())
                .containsExactly("Zulu", "Alpha", "Mike", "Bravo");
    }
}

