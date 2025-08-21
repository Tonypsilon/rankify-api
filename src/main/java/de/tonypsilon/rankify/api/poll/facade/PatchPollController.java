package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.infrastucture.transaction.Command;
import de.tonypsilon.rankify.api.poll.business.ChangePollStateUseCase;
import de.tonypsilon.rankify.api.poll.business.PollId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController()
public class PatchPollController {

    private final ChangePollStateUseCase changePollStateUseCase;

    public PatchPollController(final ChangePollStateUseCase changePollStateUseCase) {
        this.changePollStateUseCase = changePollStateUseCase;
    }

    @PatchMapping("polls/{pollId}")
    @Command
    public ResponseEntity<Void> patchPoll(@PathVariable final UUID pollId,
                                          @RequestBody final PatchPollCommand command) {
        if (command == null) {
            return ResponseEntity.badRequest().build();
        }
        switch (command.operation()) {
            case START_VOTING -> changePollStateUseCase.startVoting(new PollId(pollId));
            case END_VOTING -> changePollStateUseCase.endVoting(new PollId(pollId));
            case UPDATE_TITLE, UPDATE_SCHEDULE, UPDATE_OPTIONS ->
                    throw new UnsupportedOperationException("Not supported yet.");
        }
        return ResponseEntity.noContent().build();
    }
}
