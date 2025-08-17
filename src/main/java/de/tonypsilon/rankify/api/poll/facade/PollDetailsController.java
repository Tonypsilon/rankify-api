package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.infrastucture.transaction.Query;
import de.tonypsilon.rankify.api.poll.business.GetPollDetailsUseCase;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PollDetailsController {

    private final GetPollDetailsUseCase getPollDetailsUseCase;

    public PollDetailsController(final GetPollDetailsUseCase getPollDetailsUseCase) {
        this.getPollDetailsUseCase = getPollDetailsUseCase;
    }

    @GetMapping("polls/{pollId}")
    @Query
    public ResponseEntity<Poll> getPollDetails(@PathVariable final UUID pollId) {
        return ResponseEntity
                .ok(getPollDetailsUseCase.getPollDetails(new PollId(pollId)));
    }
}

