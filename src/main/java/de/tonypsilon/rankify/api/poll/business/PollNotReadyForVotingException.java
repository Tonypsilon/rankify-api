package de.tonypsilon.rankify.api.poll.business;

public class PollNotReadyForVotingException extends RuntimeException {
    public PollNotReadyForVotingException(PollId pollId) {
        super("Poll with ID " + pollId + " is not ready for voting.");
    }
}
