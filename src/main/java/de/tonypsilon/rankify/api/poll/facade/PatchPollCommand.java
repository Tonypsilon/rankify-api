package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.PollTitle;
import de.tonypsilon.rankify.api.poll.business.Schedule;

public record PatchPollCommand(PatchPollOperation operation,
                               PollTitle newTitle,
                               Schedule newSchedule,
                               Ballot newBallot) {
    public PatchPollCommand {
        if (operation == null) {
            throw new IllegalArgumentException("operation cannot be null");
        }
        switch (operation) {
            case UPDATE_TITLE -> {
                if (newTitle == null) {
                    throw new IllegalArgumentException("newTitle cannot be null for UPDATE_TITLE operation");
                }
            }
            case UPDATE_SCHEDULE -> {
                if (newSchedule == null) {
                    throw new IllegalArgumentException("newSchedule cannot be null for UPDATE_SCHEDULE operation");
                }
            }
            case UPDATE_OPTIONS -> {
                if (newBallot == null) {
                    throw new IllegalArgumentException("newBallot cannot be null for UPDATE_OPTIONS operation");
                }
            }
            case START_VOTING, END_VOTING -> {
                // No additional parameters required for these operations.
            }
        }
    }
}
