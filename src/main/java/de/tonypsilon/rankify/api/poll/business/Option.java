package de.tonypsilon.rankify.api.poll.business;

public record Option(String text) {

    public Option {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Option text must not be null or blank");
        }
    }

}
