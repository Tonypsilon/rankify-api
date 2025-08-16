package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChangePollStateUseCase {

    private final PollRepository pollRepository;

    public ChangePollStateUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    public void startVoting(final PollId pollId) {
        var poll = pollRepository.getById(pollId);
        poll.startVoting();
        pollRepository.update(poll);
    }

    public void endVoting(final PollId pollId) {
        var poll = pollRepository.getById(pollId);
        poll.endVoting();
        pollRepository.update(poll);
    }
}
