package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

@Service
public class InitiatePollUseCase {

    private final PollRepository pollRepository;

    public InitiatePollUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    public PollId initiatePoll(InitiatePollCommand command) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
