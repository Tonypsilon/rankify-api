package de.tonypsilon.rankify.api.poll.business;

public record PollTitle(String value) {

    public PollTitle {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Poll title must not be null or blank");
        }
    }

}
