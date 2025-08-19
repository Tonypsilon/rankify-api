package de.tonypsilon.rankify.api.poll.business;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Poll {

    private final PollId id;
    private PollTitle title;
    private Ballot ballot;
    private Schedule schedule;
    private final LocalDateTime created;

    public Poll(final PollId id,
                final PollTitle title,
                final Ballot ballot,
                final Schedule schedule,
                final LocalDateTime created) {
        this.id = id;
        this.title = title;
        this.ballot = ballot;
        this.schedule = schedule;
        this.created = created;
    }

    public void startVoting() {
        LocalDateTime now = LocalDateTime.now();
        if (this.state() != PollState.IN_PREPARATION) {
            throw new IllegalStateException("Cannot start voting when poll is not in preparation");
        }
        this.schedule = this.schedule.withStart(now);
    }

    public void endVoting() {
        LocalDateTime now = LocalDateTime.now();
        if (this.state() != PollState.ONGOING) {
            throw new IllegalStateException("Cannot finish voting when poll is not ongoing");
        }
        this.schedule = this.schedule.withEnd(now);
    }

    private PollState state() {
        LocalDateTime now = LocalDateTime.now();
        if (schedule.end() != null && schedule.end().isBefore(now)) {
            return PollState.FINISHED;
        }
        if (schedule.start() == null || schedule.start().isAfter(now)) {
            return PollState.IN_PREPARATION;
        }
        return PollState.ONGOING;
    }

    public PollId id() {
        return id;
    }

    public PollTitle title() {
        return title;
    }

    @Nonnull
    public Ballot ballot() {
        if (ballot == null) {
            throw new IllegalStateException("Ballot is not initialized");
        }
        return ballot;
    }

    public Schedule schedule() {
        return schedule;
    }

    public LocalDateTime created() {
        return created;
    }

    /**
     * Casts a vote for this poll with the given rankings.
     * The rankings must contain only options from the ballot, and each option must have a rank.
     * If an option is not ranked, it will be assigned a sentinel rank.
     *
     * @param rankings A map of options to their ranks.
     * @return A new {@link Vote} instance representing the cast vote.
     * @throws PollNotReadyForVotingException if the poll is not in an ongoing state.
     * @throws IllegalArgumentException       if any option in the rankings does not belong to this poll's ballot.
     */
    public Vote castVote(Map<Option, Integer> rankings) {
        if (state() != PollState.ONGOING) {
            throw new PollNotReadyForVotingException(id);
        }
        Objects.requireNonNull(rankings, "rankings must not be null");
        // Validate supplied options & ranks
        Map<Option, Integer> completedRankings = new LinkedHashMap<>();
        for (Map.Entry<Option, Integer> entry : rankings.entrySet()) {
            Option option = entry.getKey();
            if (!ballot().options().contains(option)) {
                throw new IllegalArgumentException("Option " + option + " does not belong to this poll's ballot");
            }
            completedRankings.put(option, entry.getValue());
        }
        // Add missing options with sentinel rank
        for (Option ballotOption : ballot().options()) {
            completedRankings.putIfAbsent(ballotOption, RecordedVote.MAX_RANKING);
        }
        return new RecordedVote(id, completedRankings);
    }
}
