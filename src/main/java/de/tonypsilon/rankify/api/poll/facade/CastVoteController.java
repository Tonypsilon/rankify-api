package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.infrastucture.transaction.Command;
import de.tonypsilon.rankify.api.poll.business.CastVoteUseCase;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.PollId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
class CastVoteController {

    private final CastVoteUseCase castVoteUseCase;

    CastVoteController(CastVoteUseCase castVoteUseCase) {
        this.castVoteUseCase = castVoteUseCase;
    }

    @PostMapping("/polls/{pollId}/votes")
    @Command
    public ResponseEntity<Void> castVote(@PathVariable("pollId") UUID pollId,
                                         @RequestBody CastVoteCommand castVoteCommand) {
        if (castVoteCommand == null || castVoteCommand.rankings() == null) {
            return ResponseEntity.badRequest().build();
        }
        Map<Option, Integer> rankings = new LinkedHashMap<>();
        castVoteCommand.rankings().forEach((k, v) -> {
            if (k != null && v != null) {
                rankings.put(new Option(k), v);
            }
        });
        castVoteUseCase.castVote(new PollId(pollId), rankings);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}

