package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollNotFoundException;
import de.tonypsilon.rankify.api.poll.business.PollTitle;
import de.tonypsilon.rankify.api.poll.business.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Comparator;
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
@Import({PollMapper.class, JpaPollRepository.class})
@Testcontainers
class JpaPollRepositoryTest {

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
        Poll poll = newPollWithOptions("Lunch", optionTexts, LocalDateTime.now().minusHours(1), null);

        PollId createdId = repository.create(poll);
        assertThat(createdId).isNotNull();
        assertThat(createdId.value()).isEqualTo(poll.id().value());

        Poll loaded = repository.getById(createdId);
        assertThat(loaded.id().value()).isEqualTo(poll.id().value());
        assertThat(loaded.title().value()).isEqualTo(poll.title().value());
        assertThat(loaded.ballot().options().stream().map(Option::text).toList())
                .containsExactlyElementsOf(sorted(optionTexts));
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
    void updateReplacesOptionsAndScheduleReflectingSortedOptionOrder() {
        Poll poll = newPollWithOptions("Color", List.of("Red", "Blue"), null, null);
        PollId id = repository.create(poll);

        List<String> newOptions = List.of("Green", "Yellow", "Purple");
        Schedule newSchedule = new Schedule(LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Poll updated = cloneChanging(poll, newOptions, newSchedule);

        repository.update(updated);
        Poll loaded = repository.getById(id);

        assertThat(loaded.ballot().options()).hasSize(newOptions.size());
        assertThat(loaded.ballot().options().stream().map(Option::text).toList())
                .containsExactlyElementsOf(sorted(newOptions));
        assertThat(loaded.schedule().start()).isEqualTo(newSchedule.start());
        assertThat(loaded.schedule().end()).isEqualTo(newSchedule.end());
    }

    @Test
    void getByIdUnknownThrows() {
        PollId random = new PollId(UUID.randomUUID());
        assertThatThrownBy(() -> repository.getById(random))
                .isInstanceOf(PollNotFoundException.class);
    }

    @Test
    void createPollWithEndTimePersistsEndTime() {
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        Poll poll = newPollWithOptions("Meeting", List.of("Topic A", "Topic B"), start, end);
        repository.create(poll);
        Poll loaded = repository.getById(poll.id());
        assertThat(loaded.schedule().end()).isEqualTo(end);
        assertThat(loaded.schedule().start()).isEqualTo(start);
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

    private Poll newPollWithOptions(String title, List<String> optionTexts, LocalDateTime start, LocalDateTime end) {
        PollId id = new PollId();
        Ballot ballot = new Ballot(optionTexts.stream().map(Option::new).toList());
        Schedule schedule = new Schedule(start, end);
        return new Poll(id, new PollTitle(title), ballot, schedule, LocalDateTime.now());
    }

    private Poll cloneChanging(Poll original, List<String> optionTexts, Schedule schedule) {
        return new Poll(original.id(), original.title(), new Ballot(optionTexts.stream().map(Option::new).toList()), schedule, original.created());
    }

    private List<String> sorted(List<String> texts) {
        return texts.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
