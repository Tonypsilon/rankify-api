package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

@Service
public class GetPollDetailsUseCase {

    private final PollRepository pollRepository;

    public GetPollDetailsUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    public Poll getPollDetails(final PollId pollId) {
        return pollRepository.getById(pollId);
    }
}
