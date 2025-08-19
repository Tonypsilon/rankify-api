package de.tonypsilon.rankify.api.poll.facade;

import de.tonypsilon.rankify.api.infrastucture.transaction.Query;
import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.GetBallotUseCase;
import de.tonypsilon.rankify.api.poll.business.PollId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GetBallotController {

    private final GetBallotUseCase getBallotUseCase;

    public GetBallotController(final GetBallotUseCase getBallotUseCase) {
        this.getBallotUseCase = getBallotUseCase;
    }

    @GetMapping("/polls/{pollId}/ballot")
    @Query
    public ResponseEntity<Ballot> getBallot(@PathVariable("pollId") final UUID pollId) {
        return ResponseEntity.ok(getBallotUseCase.getBallot(new PollId(pollId)));
    }
}
