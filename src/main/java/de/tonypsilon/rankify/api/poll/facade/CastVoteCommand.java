package de.tonypsilon.rankify.api.poll.facade;

import java.util.Map;

public record CastVoteCommand(Map<String, Integer> rankings) {
}
