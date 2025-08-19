package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RecordedVoteTest {

    @Test
    void shouldThrowExceptionForNullPollId() {
        Map<Option, Integer> rankings = Map.of(new Option("A"), 1);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(null, rankings))
                .withMessage("pollId must not be null");
    }

    @Test
    void shouldThrowExceptionForNullRankings() {
        PollId pollId = new PollId(UUID.randomUUID());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(pollId, null))
                .withMessage("rankings must not be null");
    }

    @Test
    void shouldThrowExceptionForNullOptionEntry() {
        PollId pollId = new PollId(UUID.randomUUID());
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(new Option("A"), 1);
        rankings.put(null, 2); // invalid
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(pollId, rankings))
                .withMessage("Option and ranking must not be null");
    }

    @Test
    void shouldThrowExceptionForNullRankValue() {
        PollId pollId = new PollId(UUID.randomUUID());
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        rankings.put(new Option("A"), null); // invalid
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(pollId, rankings))
                .withMessage("Option and ranking must not be null");
    }

    @Test
    void shouldThrowExceptionForRankBelowMinimum() {
        PollId pollId = new PollId(UUID.randomUUID());
        Map<Option, Integer> rankings = Map.of(new Option("A"), 0); // invalid
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(pollId, rankings))
                .withMessageContaining("must be between 1 and " + RecordedVote.MAX_RANKING);
    }

    @Test
    void shouldThrowExceptionForRankAboveMaximum() {
        PollId pollId = new PollId(UUID.randomUUID());
        Map<Option, Integer> rankings = Map.of(new Option("A"), RecordedVote.MAX_RANKING + 1); // invalid
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RecordedVote(pollId, rankings))
                .withMessageContaining("must be between 1 and " + RecordedVote.MAX_RANKING);
    }

    @Test
    void shouldCreateRecordedVoteWithValidRankingsIncludingBoundaryValues() {
        PollId pollId = new PollId(UUID.randomUUID());
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        Option a = new Option("A");
        Option b = new Option("B");
        rankings.put(a, 1); // minimum
        rankings.put(b, RecordedVote.MAX_RANKING); // maximum

        RecordedVote vote = new RecordedVote(pollId, rankings);

        assertThat(vote.pollId()).isEqualTo(pollId);
        assertThat(vote.rankings()).containsExactlyInAnyOrderEntriesOf(rankings);
    }

    @Test
    void shouldDefensivelyCopyAndBeImmutable() {
        PollId pollId = new PollId(UUID.randomUUID());
        LinkedHashMap<Option, Integer> rankings = new LinkedHashMap<>();
        Option a = new Option("A");
        Option b = new Option("B");
        rankings.put(a, 1);
        rankings.put(b, 2);

        RecordedVote vote = new RecordedVote(pollId, rankings);

        // Mutate original map after construction
        rankings.put(new Option("C"), 3);
        assertThat(vote.rankings()).hasSize(2); // unaffected

        // rankings() returns an unmodifiable defensive copy each call
        Map<Option, Integer> exposed = vote.rankings();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> exposed.put(new Option("D"), 4));

        // Each call returns an independent unmodifiable map
        Map<Option, Integer> exposed2 = vote.rankings();
        assertThat(exposed2).containsAllEntriesOf(Map.of(a, 1, b, 2));
    }
}
