package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollTitle;
import de.tonypsilon.rankify.api.poll.business.Schedule;
import de.tonypsilon.rankify.api.poll.business.Vote;
import liquibase.integration.spring.SpringLiquibase;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PollMapper.class, JpaPollRepository.class, JpaVoteRepository.class, JpaVoteRepositoryTest.LiquibaseTestConfiguration.class})
@Testcontainers
class JpaVoteRepositoryTest {

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
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private JpaPollRepository pollRepository;

    @Autowired
    private SpringDataVoteRepository springDataVoteRepository;

    @Autowired
    private JpaVoteRepository jpaVoteRepository;

    @Test
    void saveVotePersistsVoteAndRankings() {
        // Arrange: create poll with options
        List<String> optionTexts = List.of("Alpha", "Beta", "Gamma");
        Poll poll = newPollWithOptions("Test Poll", optionTexts);
        pollRepository.create(poll);

        // Build vote rankings
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        int rank = 1;
        for (String txt : optionTexts) {
            rankings.put(new Option(txt), rank++);
        }
        Vote vote = new TestVote(poll.id(), rankings);

        // Act
        pollRepository.saveVote(vote);

        // Assert
        var votes = springDataVoteRepository.findAll();
        assertThat(votes).hasSize(1);
        var stored = votes.getFirst();
        assertThat(stored.getPollId()).isEqualTo(poll.id().value());
        assertThat(stored.getRankings()).hasSize(optionTexts.size());
        assertThat(stored.getRankings().stream().map(r -> r.getOption().getText()).toList())
                .containsExactlyInAnyOrderElementsOf(optionTexts);
    }

    @Test
    void saveNullVoteThrows() {
        assertThatThrownBy(() -> jpaVoteRepository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vote must not be null");
    }

    @Test
    void saveVoteWithNullOptionThrows() {
        Poll poll = newPollWithOptions("Null Option Poll", List.of("Alpha", "Beta"));
        pollRepository.create(poll);
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(null, 1);
        Vote vote = new TestVote(poll.id(), rankings);
        assertThatThrownBy(() -> jpaVoteRepository.save(vote))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Option and rank must not be null");
    }

    @Test
    void saveVoteWithNullRankThrows() {
        Poll poll = newPollWithOptions("Null Rank Poll", List.of("Alpha", "Beta"));
        pollRepository.create(poll);
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(new Option("Alpha"), null);
        Vote vote = new TestVote(poll.id(), rankings);
        assertThatThrownBy(() -> jpaVoteRepository.save(vote))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Option and rank must not be null");
    }

    @Test
    void saveVoteWithUnknownOptionThrows() {
        Poll poll = newPollWithOptions("Unknown Option Poll", List.of("Alpha", "Beta"));
        pollRepository.create(poll);
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(new Option("NotPresent"), 1); // not in poll
        Vote vote = new TestVote(poll.id(), rankings);
        assertThatThrownBy(() -> jpaVoteRepository.save(vote))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Option not found for poll: NotPresent");
    }

    private Poll newPollWithOptions(String title, List<String> optionTexts) {
        PollId id = new PollId(UUID.randomUUID());
        Ballot ballot = new Ballot(optionTexts.stream().map(Option::new).toList());
        Schedule schedule = new Schedule(Instant.now().minusSeconds(300), Instant.now().plusSeconds(300));
        return new Poll(id, new PollTitle(title), ballot, schedule, Instant.now());
    }

    // Simple test implementation of Vote interface
    private record TestVote(PollId pollId, Map<Option, Integer> rankings) implements Vote {
        @Override
        public Map<Option, Integer> rankings() {
            return rankings;
        }
    }
}
