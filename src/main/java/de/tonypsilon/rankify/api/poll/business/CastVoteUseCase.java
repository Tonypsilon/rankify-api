package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class CastVoteUseCase {

    private final PollRepository pollRepository;
    private final VoteFactory voteFactory;

    public CastVoteUseCase(final PollRepository pollRepository, final VoteFactory voteFactory) {
        this.pollRepository = pollRepository;
        this.voteFactory = voteFactory;
    }

    public void castVote(PollId pollId, Map<Option, Integer> rankings) {
        if (pollId == null) {
            throw new IllegalArgumentException("pollId must not be null");
        }
        Objects.requireNonNull(rankings, "rankings must not be null");
        Poll poll = pollRepository.getById(pollId);
        Vote vote = voteFactory.createVote(poll, rankings);
        pollRepository.saveVote(vote);
    }
}
