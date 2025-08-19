package de.tonypsilon.rankify.api.poll.business;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

@Service
public class GetBallotUseCase {

    private final PollRepository pollRepository;

    public GetBallotUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    /**
     * Retrieves the ballot for a given poll.
     *
     * @param pollId the unique identifier of the poll
     * @return the ballot associated with the specified poll
     * @throws IllegalArgumentException if the pollId is null or does not exist in the repository
     */
    @Nonnull
    public Ballot getBallot(PollId pollId) {
        return pollRepository.getById(pollId).ballot();
    }
}
