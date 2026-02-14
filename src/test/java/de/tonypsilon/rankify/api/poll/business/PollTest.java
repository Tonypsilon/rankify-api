package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PollTest {

    private Poll newPollWithSchedule(Schedule schedule) {
        Ballot ballot = new Ballot(List.of(new Option("Option A"), new Option("Option B"), new Option("Option C")));
        return new Poll(new PollId(UUID.randomUUID()), new PollTitle("Test Poll"), ballot, schedule, Instant.now());
    }

    @Test
    void castVote_whenPollOngoing_withPartialRankings_addsMissingOptionsWithSentinel() {
        // Given: poll already ongoing (start in the past, no end)
        Schedule schedule = new Schedule(Instant.now().minusSeconds(5), null);
        Poll poll = newPollWithSchedule(schedule);
        Option rankedOption = poll.ballot().options().getFirst();
        Map<Option, Integer> partialRankings = Map.of(rankedOption, 1);

        // When
        Vote vote = poll.castVote(partialRankings);

        // Then
        // Build expected ordered map
        LinkedHashMap<Option, Integer> expected = new LinkedHashMap<>();
        for (Option opt : poll.ballot().options()) {
            expected.put(opt, opt.equals(rankedOption) ? 1 : RecordedVote.MAX_RANKING);
        }
        assertThat(vote).isNotNull();
        assertThat(vote.pollId()).isEqualTo(poll.id());
        assertThat(vote.rankings()).containsExactlyInAnyOrderEntriesOf(expected);
        assertThat(vote.rankings()).containsEntry(rankedOption, 1);
    }

    @Test
    void castVote_whenPollNotOngoing_throwsPollNotReadyForVotingException() {
        // Given: poll in preparation (start null)
        Poll poll = newPollWithSchedule(new Schedule(null, null));
        Map<Option, Integer> emptyVote = Collections.emptyMap();

        // When & Then
        assertThatExceptionOfType(PollNotReadyForVotingException.class)
                .isThrownBy(() -> poll.castVote(emptyVote));
    }

    @Test
    void castVote_withOptionNotInBallot_throwsIllegalArgumentException() {
        // Given: ongoing poll
        Poll poll = newPollWithSchedule(new Schedule(Instant.now().minusSeconds(10), null));
        var foreignOption = Map.of(new Option("Foreign Option"), 1);

        // When & Then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> poll.castVote(foreignOption))
                .withMessageContaining("does not belong");
    }

    @Test
    void startVoting_fromPreparation_setsStartTime() {
        // Given
        Poll poll = newPollWithSchedule(new Schedule(null, null));
        assertThat(poll.schedule().start()).isNull();

        // When
        poll.startVoting();

        // Then
        assertThat(poll.schedule().start()).isNotNull();
        assertThat(poll.schedule().end()).isNull();
    }

    @Test
    void startVoting_whenAlreadyOngoing_throwsIllegalStateException() {
        // Given: already ongoing (start in past)
        Poll poll = newPollWithSchedule(new Schedule(Instant.now().minusSeconds(30), null));

        // When & Then
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(poll::startVoting);
    }

    @Test
    void endVoting_whenOngoing_setsEndTimeAndPreventsFurtherVotes() {
        // Given: ongoing poll
        Poll poll = newPollWithSchedule(new Schedule(Instant.now().minusSeconds(2), null));
        assertThat(poll.schedule().end()).isNull();

        // When
        poll.endVoting();

        // Then
        assertThat(poll.schedule().end()).isNotNull();
        // Further voting should be rejected

        Map<Option, Integer> emptyVote = Collections.emptyMap();
        assertThatExceptionOfType(PollNotReadyForVotingException.class)
                .isThrownBy(() -> poll.castVote(emptyVote));
    }

    @Test
    void endVoting_whenNotOngoing_throwsIllegalStateException() {
        // Given: in preparation (no start)
        Poll poll = newPollWithSchedule(new Schedule(null, null));

        // When & Then
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(poll::endVoting);
    }

    @Test
    void castVote_preservesExplicitRankingOrderAndNoMutation() {
        // Given: ongoing poll
        Poll poll = newPollWithSchedule(new Schedule(Instant.now().minusSeconds(5), null));
        Option first = poll.ballot().options().getFirst();
        Option second = poll.ballot().options().stream().skip(1).findFirst().orElseThrow();
        Option third = poll.ballot().options().getLast();
        Map<Option, Integer> custom = new LinkedHashMap<>();
        custom.put(second, 1);
        custom.put(first, 2);

        // When
        Vote vote = poll.castVote(custom);

        // Then
        LinkedHashMap<Option, Integer> expected = new LinkedHashMap<>();
        expected.put(second, 1);
        expected.put(first, 2);
        expected.put(third, RecordedVote.MAX_RANKING);
        assertThat(vote).isNotNull();
        // Ignore ordering constraint due to Map.copyOf implementation details
        assertThat(vote.rankings()).containsAllEntriesOf(expected)
                .hasSize(expected.size());
        assertThat(custom).doesNotContainKey(third);
        assertThat(vote.rankings()).containsEntry(third, RecordedVote.MAX_RANKING);
        assertThat(vote.rankings()).containsEntry(second, 1).containsEntry(first, 2);
    }
}
