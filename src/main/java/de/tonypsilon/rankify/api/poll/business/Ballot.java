package de.tonypsilon.rankify.api.poll.business;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

public record Ballot(SequencedSet<Option> options) {

    public Ballot(List<Option> optionsList) {
        this(validateAndCreateSequencedSet(optionsList));
    }

    public Ballot {
        if (options == null) {
            throw new IllegalArgumentException("Options must not be null");
        }
        if (options.size() < 2) {
            throw new IllegalArgumentException("Ballot must have at least two options");
        }
        // Validate no null or blank options (Option constructor already handles this)
        for (Option option : options) {
            if (option == null) {
                throw new IllegalArgumentException("Option must not be null");
            }
        }
    }

    public SequencedSet<Option> options() {
        // Return defensive copy
        return new LinkedHashSet<>(options);
    }

    private static SequencedSet<Option> validateAndCreateSequencedSet(List<Option> optionsList) {
        if (optionsList == null) {
            throw new IllegalArgumentException("Options list must not be null");
        }
        if (optionsList.size() < 2) {
            throw new IllegalArgumentException("Ballot must have at least two options");
        }

        SequencedSet<Option> optionsSet = new LinkedHashSet<>();
        for (Option option : optionsList) {
            if (option == null) {
                throw new IllegalArgumentException("Option must not be null");
            }
            boolean wasAdded = optionsSet.add(option);
            if (!wasAdded) {
                throw new IllegalArgumentException("Duplicate option found: " + option.text());
            }
        }
        return optionsSet;
    }
}
