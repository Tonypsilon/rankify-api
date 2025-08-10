package de.tonypsilon.rankify.api.poll.business;

public class PollNotFoundException extends RuntimeException {
    public PollNotFoundException(PollId pollId) {
        super("Poll not found with ID: " + pollId);
    }
}
