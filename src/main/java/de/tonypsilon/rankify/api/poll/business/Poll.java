package de.tonypsilon.rankify.api.poll.business;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;

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
}
