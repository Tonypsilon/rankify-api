package de.tonypsilon.rankify.api.poll.business;

import java.util.UUID;

public record PollId(UUID value) {

    public PollId {
        if (value == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }
    }

    public PollId() {
        this(UUID.randomUUID());
    }
}
