package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BallotTest {

    @Test
    void testConstructorOverloadingWithList() {
        // Given: A List of options
        List<Option> optionsList = List.of(
                new Option("Option A"),
                new Option("Option B")
        );

        // When: Creating a Ballot with the List constructor
        Ballot ballot = new Ballot(optionsList);

        // Then: The ballot should be created successfully
        assertThat(ballot).isNotNull();
        assertThat(ballot.options())
                .containsExactly(new Option("Option A"), new Option("Option B"));
    }

    @Test
    void testConstructorOverloadingWithSequencedSet() {
        // Given: A SequencedSet of options
        SequencedSet<Option> optionsSet = new LinkedHashSet<>();
        optionsSet.add(new Option("Option B"));
        optionsSet.add(new Option("Option A"));

        // When: Creating a Ballot with the SequencedSet constructor
        Ballot ballot = new Ballot(optionsSet);

        // Then: The ballot should be created successfully
        assertThat(ballot).isNotNull();
        assertThat(ballot.options())
                .hasSize(2)
                .contains(new Option("Option B"), new Option("Option A"));
    }

    @Test
    void testConstructorAmbiguityWithArrayList() {
        // Given: An ArrayList (implements List but not SequencedSet)
        ArrayList<Option> optionsList = new ArrayList<>();
        optionsList.add(new Option("Option A"));
        optionsList.add(new Option("Option B"));

        // When: Creating a Ballot - this should use the List constructor
        Ballot ballot = new Ballot(optionsList);

        // Then: The ballot should be created successfully
        assertThat(ballot).isNotNull();
        assertThat(ballot.options()).hasSize(2);
    }

    @Test
    void testConstructorAmbiguityWithLinkedHashSet() {
        // Given: A LinkedHashSet (implements SequencedSet)
        LinkedHashSet<Option> optionsSet = new LinkedHashSet<>();
        optionsSet.add(new Option("Option A"));
        optionsSet.add(new Option("Option B"));

        // When: Creating a Ballot - this should use the SequencedSet constructor
        Ballot ballot = new Ballot(optionsSet);

        // Then: The ballot should be created successfully
        assertThat(ballot).isNotNull();
        assertThat(ballot.options()).hasSize(2);
    }

    @Test
    void testListConstructorValidatesDuplicates() {
        // Given: A List with duplicate options
        List<Option> optionsWithDuplicates = List.of(
                new Option("Option A"),
                new Option("Option B"),
                new Option("Option A") // Duplicate
        );

        // When & Then: Creating a Ballot should throw an exception
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ballot(optionsWithDuplicates))
                .withMessage("Duplicate option found: Option A");
    }

    @Test
    void testSequencedSetConstructorDoesNotValidateDuplicates() {
        // Given: A SequencedSet (which inherently prevents duplicates)
        SequencedSet<Option> optionsSet = new LinkedHashSet<>();
        optionsSet.add(new Option("Option A"));
        optionsSet.add(new Option("Option B"));
        optionsSet.add(new Option("Option A")); // ignored by Set

        // When: Creating a Ballot with SequencedSet constructor
        Ballot ballot = new Ballot(optionsSet);

        // Then: The ballot should be created successfully with only unique options
        assertThat(ballot).isNotNull();
        assertThat(ballot.options()).hasSize(2); // Only 2 unique options
    }

    @Test
    void testDefensiveCopyInGetter() {
        // Given: A ballot with options
        List<Option> originalOptions = List.of(
                new Option("Option A"),
                new Option("Option B")
        );
        Ballot ballot = new Ballot(originalOptions);

        // When: Getting options and modifying the returned set
        SequencedSet<Option> retrievedOptions = ballot.options();

        // Then: Modifying the returned set should not affect the original ballot
        assertThatCode(retrievedOptions::clear).doesNotThrowAnyException();
        assertThat(ballot.options()).hasSize(2); // Original ballot unchanged
    }

    @Test
    void testMinimumTwoOptionsValidation() {
        // Given: A List with only one option
        List<Option> singleOption = List.of(new Option("Only Option"));

        // When & Then: Creating a Ballot should throw an exception
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ballot(singleOption))
                .withMessage("Ballot must have at least two options");
    }

    @Test
    void testNullOptionValidation() {
        // Given: A List with a null option
        List<Option> optionsWithNull = new ArrayList<>();
        optionsWithNull.add(new Option("Option A"));
        optionsWithNull.add(null);

        // When & Then: Creating a Ballot should throw an exception
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ballot(optionsWithNull))
                .withMessage("Option must not be null");
    }
}
