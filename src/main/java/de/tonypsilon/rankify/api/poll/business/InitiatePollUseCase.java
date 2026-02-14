package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InitiatePollUseCase {

    private final PollRepository pollRepository;

    public InitiatePollUseCase(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    /**
     * Initiates a new poll by creating a Poll entity with the provided command data.
     * The poll is created with a new unique ID and the current timestamp as creation time.
     * The poll will be persisted in the repository and its ID will be returned.
     *
     * @param command the InitiatePollCommand containing the poll title, ballot, and schedule
     * @return the unique PollId of the newly created poll
     * @throws IllegalArgumentException if the command is null
     * @throws IllegalArgumentException if any of the command's required fields (title, ballot, schedule) are invalid
     *                                  as validated by the repository during creation
     */
    public PollId initiatePoll(InitiatePollCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command must not be null");
        }
        Poll poll = new Poll(
                new PollId(),
                command.title(),
                command.ballot(),
                command.schedule(),
                Instant.now()
        );
        return pollRepository.create(poll);
    }
}
