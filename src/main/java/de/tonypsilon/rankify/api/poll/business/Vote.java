package de.tonypsilon.rankify.api.poll.business;

import java.util.Map;

/**
 * Public view of a recorded vote. Instances are created via Poll.castVote(...).
 */
public interface Vote {

    PollId pollId();

    Map<Option, Integer> rankings();
}
