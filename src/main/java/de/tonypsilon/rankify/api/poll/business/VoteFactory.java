package de.tonypsilon.rankify.api.poll.business;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain service responsible for creating Vote instances with proper validation.
 * This factory encapsulates the complex logic of vote creation, including:
 * - Validating that the poll is ready to accept votes
 * - Validating that ranked options belong to the poll's ballot
 * - Completing rankings by adding unranked options with sentinel values
 * <p>
 * This separation of concerns keeps the Poll entity focused on its own lifecycle
 * while delegating vote creation to a dedicated factory following DDD patterns.
 */
@Service
public class VoteFactory {

    /**
     * Creates a vote for the given poll with the specified rankings.
     * Performs all necessary validation and completes missing rankings with sentinel values.
     *
     * @param poll     the poll for which the vote is being cast
     * @param rankings a map of options to their rankings (1-based, lower is better)
     * @return a new Vote instance representing the cast vote
     * @throws IllegalArgumentException       if poll is null, rankings is null,
     *                                        or if any ranked option does not belong to the poll's ballot
     * @throws PollNotReadyForVotingException if the poll is not in a state to accept votes
     */
    public Vote createVote(Poll poll, Map<Option, Integer> rankings) {
        Objects.requireNonNull(poll, "poll must not be null");
        Objects.requireNonNull(rankings, "rankings must not be null");

        // Validate poll is ready for voting
        if (!poll.canAcceptVotes()) {
            throw new PollNotReadyForVotingException(poll.id());
        }

        // Validate and complete rankings
        Map<Option, Integer> completedRankings = validateAndCompleteRankings(
                poll.ballot(),
                rankings
        );

        // Create the vote using package-private constructor
        return new RecordedVote(poll.id(), completedRankings);
    }

    /**
     * Validates that all ranked options belong to the ballot and completes
     * the rankings by adding unranked options with sentinel values.
     *
     * @param ballot   the ballot containing valid options
     * @param rankings the user-provided rankings to validate
     * @return a completed map containing all ballot options with their rankings
     * @throws IllegalArgumentException if any ranked option is not in the ballot
     */
    private Map<Option, Integer> validateAndCompleteRankings(Ballot ballot, Map<Option, Integer> rankings) {
        Map<Option, Integer> completedRankings = new LinkedHashMap<>();

        // Validate supplied options belong to ballot and copy to completed rankings
        for (Map.Entry<Option, Integer> entry : rankings.entrySet()) {
            Option option = entry.getKey();
            if (!ballot.options().contains(option)) {
                throw new IllegalArgumentException(
                        "Option " + option + " does not belong to this poll's ballot"
                );
            }
            completedRankings.put(option, entry.getValue());
        }

        // Add missing options with sentinel rank
        for (Option ballotOption : ballot.options()) {
            completedRankings.putIfAbsent(ballotOption, RecordedVote.MAX_RANKING);
        }

        return completedRankings;
    }
}
