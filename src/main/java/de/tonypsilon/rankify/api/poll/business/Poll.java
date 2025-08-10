package de.tonypsilon.rankify.api.poll.business;

import java.time.LocalDateTime;

public class Poll {

    private final PollId id;
    private final PollTitle title;
    private final Ballot ballot;
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

    public void finishVoting() {
        LocalDateTime now = LocalDateTime.now();
        if (this.state() != PollState.ONGOING) {
            throw new IllegalStateException("Cannot finish voting when poll is not ongoing");
        }
        this.schedule = this.schedule.withEnd(now);
    }

    private PollState state() {
        LocalDateTime now = LocalDateTime.now();

        if (schedule.start() == null || schedule.start().isAfter(now)) {
            return PollState.IN_PREPARATION;
        }

        if (schedule.end() != null && schedule.end().isBefore(now)) {
            return PollState.FINISHED;
        }

        return PollState.ONGOING;
    }
}
