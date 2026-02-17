package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollNotFoundException;
import de.tonypsilon.rankify.api.poll.business.PollTitle;
import de.tonypsilon.rankify.api.poll.business.Schedule;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration style tests for {@link JpaPollRepository} using a real PostgreSQL Testcontainer.
 * Focus: persistence round-trips, option replacement on update, existence & error behavior.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PollMapper.class, JpaPollRepository.class, JpaVoteRepository.class, JpaPollRepositoryTest.LiquibaseTestConfiguration.class})
@Testcontainers
class JpaPollRepositoryTest {

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
    @SuppressWarnings("resource") // Testcontainers manages the lifecycle of this container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("rankify_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Liquibase builds schema; keep ddl-auto none.
    }

    @Autowired
    private JpaPollRepository repository;

    @Test
    void createAndGetByIdReturnsPersistedPollWithOptionsSortedByText() {
        List<String> optionTexts = List.of("Pizza", "Sushi");
        Poll poll = newPollWithOptions("Lunch", optionTexts, Instant.now().minusSeconds(3600), null);

        PollId createdId = repository.create(poll);
        assertThat(createdId).isNotNull();
        assertThat(createdId.value()).isEqualTo(poll.id().value());

        Poll loaded = repository.getById(createdId);
        assertThat(loaded.id().value()).isEqualTo(poll.id().value());
        assertThat(loaded.title().value()).isEqualTo(poll.title().value());
        assertThat(loaded.ballot().options().stream().map(Option::text).toList())
                .containsExactlyElementsOf(optionTexts);
        assertThat(loaded.schedule().start()).isEqualTo(poll.schedule().start());
        assertThat(loaded.schedule().end()).isNull();
    }

    @Test
    void existsByIdReturnsTrueAfterCreate() {
        Poll poll = newPollWithOptions("Trip", List.of("Mountains", "Beach"), null, null);
        PollId id = repository.create(poll);
        assertThat(repository.existsById(id)).isTrue();
    }

    @Test
    void existsByIdReturnsFalseWhenNotPresent() {
        assertThat(repository.existsById(new PollId())).isFalse();
    }

    @Test
    void updateReplacesOptionsAndScheduleReflectingSortedOptionOrder() {
        Poll poll = newPollWithOptions("Color", List.of("Red", "Blue"), null, null);
        PollId id = repository.create(poll);

        List<String> newOptions = List.of("Green", "Yellow", "Purple");
        Schedule newSchedule = new Schedule(Instant.now().minusSeconds(600), Instant.now().plusSeconds(600));
        Poll updated = cloneChanging(poll, newOptions, newSchedule);

        repository.update(updated);
        Poll loaded = repository.getById(id);

        assertThat(loaded.ballot().options()).hasSize(newOptions.size());
        assertThat(loaded.ballot().options().stream().map(Option::text).toList())
                .containsExactlyElementsOf(newOptions);
        assertThat(loaded.schedule().start()).isEqualTo(newSchedule.start());
        assertThat(loaded.schedule().end()).isEqualTo(newSchedule.end());
    }

    @Test
    void updateWithSameOptionsDoesNotAlterOptions() {
        Poll poll = newPollWithOptions("Colors", List.of("Red", "Blue", "Green"), null, null);
        PollId id = repository.create(poll);

        // Re-create poll instance with same option set but reversed order (sorted lists equal) and changed schedule
        Schedule newSchedule = new Schedule(Instant.now().minusSeconds(120), Instant.now().plusSeconds(120));
        Poll reordered = new Poll(id, poll.title(), new Ballot(List.of("Green", "Blue", "Red").stream().map(Option::new).toList()), newSchedule, poll.created());

        repository.update(reordered);
        Poll loaded = repository.getById(id);

        // Expect original order preserved (since order replacement not triggered)
        assertThat(loaded.ballot().options().stream().map(Option::text).toList())
                .containsExactlyElementsOf(poll.ballot().options().stream().map(Option::text).toList());
        assertThat(loaded.schedule().start()).isEqualTo(newSchedule.start());
        assertThat(loaded.schedule().end()).isEqualTo(newSchedule.end());
    }

    @Test
    void updateUnknownPollThrowsPollNotFound() {
        Poll poll = newPollWithOptions("Unknown", List.of("A", "B"), null, null); // need >=2 options
        assertThatThrownBy(() -> repository.update(poll)).isInstanceOf(PollNotFoundException.class);
    }

    @Test
    void getByIdUnknownThrows() {
        PollId random = new PollId(UUID.randomUUID());
        assertThatThrownBy(() -> repository.getById(random))
                .isInstanceOf(PollNotFoundException.class);
    }

    @Test
    void createPollWithEndTimePersistsEndTime() {
        Instant start = Instant.now().minusSeconds(10800);
        Instant end = Instant.now().minusSeconds(3600);
        Poll poll = newPollWithOptions("Meeting", List.of("Topic A", "Topic B"), start, end);
        repository.create(poll);
        Poll loaded = repository.getById(poll.id());
        assertThat(loaded.schedule().end()).isEqualTo(end);
        assertThat(loaded.schedule().start()).isEqualTo(start);
    }

    @Test
    void saveVoteNullThrows() {
        assertThatThrownBy(() -> repository.saveVote(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vote must not be null");
    }

    @Nested
    @DisplayName("Argument validation")
    class ArgumentValidation {
        @Test
        void existsByIdNullThrows() {
            assertIllegalArgumentException(() -> repository.existsById(null), "Poll ID must not be null");
        }

        @Test
        void createNullThrows() {
            assertIllegalArgumentException(() -> repository.create(null), "Poll must not be null");
        }

        @Test
        void getByIdNullThrows() {
            assertIllegalArgumentException(() -> repository.getById(null), "Poll ID must not be null");
        }

        @Test
        void updateNullThrows() {
            assertIllegalArgumentException(() -> repository.update(null), "Poll must not be null");
        }

        private void assertIllegalArgumentException(Runnable runnable, String expectedMessage) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(runnable::run)
                    .withMessage(expectedMessage);
        }
    }

    private Poll newPollWithOptions(String title, List<String> optionTexts, Instant start, Instant end) {
        PollId id = new PollId();
        Ballot ballot = new Ballot(optionTexts.stream().map(Option::new).toList());
        Schedule schedule = new Schedule(start, end);
        return new Poll(id, new PollTitle(title), ballot, schedule, Instant.now());
    }

    private Poll cloneChanging(Poll original, List<String> optionTexts, Schedule schedule) {
        return new Poll(original.id(), original.title(), new Ballot(optionTexts.stream().map(Option::new).toList()), schedule, original.created());
    }
}
