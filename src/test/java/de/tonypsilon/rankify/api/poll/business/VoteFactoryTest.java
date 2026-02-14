package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class VoteFactoryTest {

    private VoteFactory voteFactory;
    private PollId pollId;
    private Option optionA;
    private Option optionB;
    private Option optionC;
    private Ballot ballot;

    @BeforeEach
    void setUp() {
        voteFactory = new VoteFactory();
        pollId = new PollId(UUID.randomUUID());
        optionA = new Option("Option A");
        optionB = new Option("Option B");
        optionC = new Option("Option C");
        ballot = new Ballot(List.of(optionA, optionB, optionC));
    }

    private Poll createOngoingPoll() {
        Schedule schedule = new Schedule(LocalDateTime.now().minusSeconds(10), null);
        return new Poll(pollId, new PollTitle("Test Poll"), ballot, schedule, LocalDateTime.now());
    }

    private Poll createPollInPreparation() {
        Schedule schedule = new Schedule(null, null);
        return new Poll(pollId, new PollTitle("Test Poll"), ballot, schedule, LocalDateTime.now());
    }

    private Poll createFinishedPoll() {
        Schedule schedule = new Schedule(
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(1)
        );
        return new Poll(pollId, new PollTitle("Test Poll"), ballot, schedule, LocalDateTime.now());
    }

    @Test
    void createVote_withValidOngoingPollAndPartialRankings_shouldCompleteWithSentinelValues() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When
        Vote vote = voteFactory.createVote(poll, rankings);

        // Then
        assertThat(vote).isNotNull();
        assertThat(vote.pollId()).isEqualTo(pollId);
        assertThat(vote.rankings()).hasSize(3);
        assertThat(vote.rankings()).containsEntry(optionA, 1);
        assertThat(vote.rankings()).containsEntry(optionB, RecordedVote.MAX_RANKING);
        assertThat(vote.rankings()).containsEntry(optionC, RecordedVote.MAX_RANKING);
    }

    @Test
    void createVote_withValidOngoingPollAndCompleteRankings_shouldPreserveAllRankings() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(optionA, 1);
        rankings.put(optionB, 2);
        rankings.put(optionC, 3);

        // When
        Vote vote = voteFactory.createVote(poll, rankings);

        // Then
        assertThat(vote).isNotNull();
        assertThat(vote.pollId()).isEqualTo(pollId);
        assertThat(vote.rankings()).containsExactlyInAnyOrderEntriesOf(rankings);
    }

    @Test
    void createVote_withPollInPreparation_shouldThrowPollNotReadyForVotingException() {
        // Given
        Poll poll = createPollInPreparation();
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When & Then
        assertThatExceptionOfType(PollNotReadyForVotingException.class)
                .isThrownBy(() -> voteFactory.createVote(poll, rankings));
    }

    @Test
    void createVote_withFinishedPoll_shouldThrowPollNotReadyForVotingException() {
        // Given
        Poll poll = createFinishedPoll();
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When & Then
        assertThatExceptionOfType(PollNotReadyForVotingException.class)
                .isThrownBy(() -> voteFactory.createVote(poll, rankings));
    }

    @Test
    void createVote_withNullPoll_shouldThrowNullPointerException() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When & Then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> voteFactory.createVote(null, rankings))
                .withMessageContaining("poll must not be null");
    }

    @Test
    void createVote_withNullRankings_shouldThrowNullPointerException() {
        // Given
        Poll poll = createOngoingPoll();

        // When & Then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> voteFactory.createVote(poll, null))
                .withMessageContaining("rankings must not be null");
    }

    @Test
    void createVote_withOptionNotInBallot_shouldThrowIllegalArgumentException() {
        // Given
        Poll poll = createOngoingPoll();
        Option foreignOption = new Option("Foreign Option");
        Map<Option, Integer> rankings = Map.of(foreignOption, 1);

        // When & Then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> voteFactory.createVote(poll, rankings))
                .withMessageContaining("does not belong to this poll's ballot");
    }

    @Test
    void createVote_withMixedValidAndInvalidOptions_shouldThrowIllegalArgumentException() {
        // Given
        Poll poll = createOngoingPoll();
        Option foreignOption = new Option("Foreign Option");
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(optionA, 1);
        rankings.put(foreignOption, 2);

        // When & Then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> voteFactory.createVote(poll, rankings))
                .withMessageContaining("does not belong to this poll's ballot");
    }

    @Test
    void createVote_withEmptyRankings_shouldCreateVoteWithAllSentinelValues() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = Collections.emptyMap();

        // When
        Vote vote = voteFactory.createVote(poll, rankings);

        // Then
        assertThat(vote).isNotNull();
        assertThat(vote.pollId()).isEqualTo(pollId);
        assertThat(vote.rankings()).hasSize(3);
        assertThat(vote.rankings()).containsEntry(optionA, RecordedVote.MAX_RANKING);
        assertThat(vote.rankings()).containsEntry(optionB, RecordedVote.MAX_RANKING);
        assertThat(vote.rankings()).containsEntry(optionC, RecordedVote.MAX_RANKING);
    }

    @Test
    void createVote_shouldReturnVoteImplementingInterface() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When
        Vote vote = voteFactory.createVote(poll, rankings);

        // Then
        assertThat(vote).isInstanceOf(Vote.class);
        // Should not leak implementation details
        assertThat(vote.pollId()).isNotNull();
        assertThat(vote.rankings()).isNotNull();
    }

    @Test
    void createVote_withCustomRankingOrder_shouldPreserveProvidedRankings() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(optionB, 1);  // B is first choice
        rankings.put(optionA, 2);  // A is second choice
        // C is not ranked, should get sentinel

        // When
        Vote vote = voteFactory.createVote(poll, rankings);

        // Then
        assertThat(vote.rankings()).containsEntry(optionB, 1);
        assertThat(vote.rankings()).containsEntry(optionA, 2);
        assertThat(vote.rankings()).containsEntry(optionC, RecordedVote.MAX_RANKING);
    }

    @Test
    void createVote_multipleTimes_shouldCreateIndependentVotes() {
        // Given
        Poll poll = createOngoingPoll();
        Map<Option, Integer> rankings1 = Map.of(optionA, 1);
        Map<Option, Integer> rankings2 = Map.of(optionB, 1);

        // When
        Vote vote1 = voteFactory.createVote(poll, rankings1);
        Vote vote2 = voteFactory.createVote(poll, rankings2);

        // Then
        assertThat(vote1.rankings()).isNotEqualTo(vote2.rankings());
        assertThat(vote1.rankings()).containsEntry(optionA, 1);
        assertThat(vote2.rankings()).containsEntry(optionB, 1);
    }
}
