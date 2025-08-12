package de.tonypsilon.rankify.api.poll.business;

public record InitiatePollCommand(PollTitle title,
                                  Ballot ballot,
                                  Schedule schedule) {
}
