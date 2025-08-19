package de.tonypsilon.rankify.api.poll.business;

public class CastVoteUseCase {

    private final PollRepository pollRepository;

    public CastVoteUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }


    public void castVote(PollId pollId, String voterId, Option option) {

    }
}
