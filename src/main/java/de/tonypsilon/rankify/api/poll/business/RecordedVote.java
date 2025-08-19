package de.tonypsilon.rankify.api.poll.business;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Package-private immutable implementation created only inside the poll package.
 */
record RecordedVote(PollId pollId, Map<Option, Integer> rankings) implements Vote {

    static int MAX_RANKING = 10000; // sentinel for unranked options (lowest priority)

    /**
     * Creates a new RecordedVote instance. This constructor is package-private and should only be used
     * within the poll package, specifically by Poll.castVote(...).
     * <p>
     * This constructor validates the input parameters:
     * - pollId must not be null.
     * - rankings must not be null.
     * - Each option in rankings must not be null.
     * - Each ranking must be between 1 and MAX_RANKING (inclusive).
     * - Each option must be unique in the rankings map.
     *
     * @param pollId   the ID of the poll this vote belongs to
     * @param rankings a map of options to their rankings, where the key is the option and the value is the ranking (1-based)
     */
    RecordedVote {
        if (pollId == null) {
            throw new IllegalArgumentException("pollId must not be null");
        }
        if (rankings == null) {
            throw new IllegalArgumentException("rankings must not be null");
        }
        // Validate entries & make defensive unmodifiable copy preserving insertion order
        LinkedHashMap<Option, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<Option, Integer> e : rankings.entrySet()) {
            Option option = e.getKey();
            Integer rank = e.getValue();
            if (option == null || rank == null) {
                throw new IllegalArgumentException("Option and ranking must not be null");
            }
            if (rank < 1 || rank > MAX_RANKING) {
                throw new IllegalArgumentException("Ranking for option " + option + " must be between 1 and " + MAX_RANKING);
            }
            if (copy.put(option, rank) != null) { // duplicate detection
                throw new IllegalArgumentException("Option " + option + " appears multiple times");
            }
        }
        rankings = Collections.unmodifiableMap(copy);
    }

    /**
     * Defensive copy
     *
     * @return an unmodifiable map of options to their rankings
     */
    @Override
    public Map<Option, Integer> rankings() {
        return Map.copyOf(rankings);
    }
}
