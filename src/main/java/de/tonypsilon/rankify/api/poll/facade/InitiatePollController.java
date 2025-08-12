package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.infrastucture.transaction.Command;
import de.tonypsilon.rankify.api.poll.business.InitiatePollCommand;
import de.tonypsilon.rankify.api.poll.business.InitiatePollUseCase;
import de.tonypsilon.rankify.api.poll.business.PollId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController("polls")
public class InitiatePollController {

    private final InitiatePollUseCase initiatePollUseCase;

    public InitiatePollController(final InitiatePollUseCase initiatePollUseCase) {
        this.initiatePollUseCase = initiatePollUseCase;
    }

    /**
     * Initiates a new poll based on the provided command data.
     * Creates a new poll with the specified title, ballot configuration, and schedule.
     *
     * @param command the command containing poll title, ballot, and schedule information
     * @return ResponseEntity containing the UUID of the newly created poll with HTTP 201 status
     * @throws IllegalArgumentException if the command is null or contains invalid data
     */
    @PostMapping
    @Command
    public ResponseEntity<UUID> initiatePoll(@RequestBody InitiatePollCommand command) {
        PollId pollId = initiatePollUseCase.initiatePoll(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pollId.value());
    }
}
