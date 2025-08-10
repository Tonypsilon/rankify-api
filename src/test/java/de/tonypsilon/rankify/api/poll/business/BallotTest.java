package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNotNull(ballot);
        assertEquals(2, ballot.options().size());
        assertTrue(ballot.options().contains(new Option("Option A")));
        assertTrue(ballot.options().contains(new Option("Option B")));
    }

    @Test
    void testConstructorOverloadingWithSequencedSet() {
        // Given: A SequencedSet of options
        SequencedSet<Option> optionsSet = new LinkedHashSet<>();
        optionsSet.add(new Option("Option A"));
        optionsSet.add(new Option("Option B"));

        // When: Creating a Ballot with the SequencedSet constructor
        Ballot ballot = new Ballot(optionsSet);

        // Then: The ballot should be created successfully
        assertNotNull(ballot);
        assertEquals(2, ballot.options().size());
        assertTrue(ballot.options().contains(new Option("Option A")));
        assertTrue(ballot.options().contains(new Option("Option B")));
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
        assertNotNull(ballot);
        assertEquals(2, ballot.options().size());
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
        assertNotNull(ballot);
        assertEquals(2, ballot.options().size());
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
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Ballot(optionsWithDuplicates)
        );
        assertEquals("Duplicate option found: Option A", exception.getMessage());
    }

    @Test
    void testSequencedSetConstructorDoesNotValidateDuplicates() {
        // Given: A SequencedSet (which inherently prevents duplicates)
        SequencedSet<Option> optionsSet = new LinkedHashSet<>();
        optionsSet.add(new Option("Option A"));
        optionsSet.add(new Option("Option B"));
        // Attempt to add duplicate - Set will ignore it
        optionsSet.add(new Option("Option A"));

        // When: Creating a Ballot with SequencedSet constructor
        Ballot ballot = new Ballot(optionsSet);

        // Then: The ballot should be created successfully with only unique options
        assertNotNull(ballot);
        assertEquals(2, ballot.options().size()); // Only 2 unique options
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
        assertDoesNotThrow(retrievedOptions::clear);
        assertEquals(2, ballot.options().size()); // Original ballot unchanged
    }

    @Test
    void testMinimumTwoOptionsValidation() {
        // Given: A List with only one option
        List<Option> singleOption = List.of(new Option("Only Option"));

        // When & Then: Creating a Ballot should throw an exception
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Ballot(singleOption)
        );
        assertEquals("Ballot must have at least two options", exception.getMessage());
    }

    @Test
    void testNullOptionValidation() {
        // Given: A List with a null option
        List<Option> optionsWithNull = new ArrayList<>();
        optionsWithNull.add(new Option("Option A"));
        optionsWithNull.add(null);

        // When & Then: Creating a Ballot should throw an exception
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Ballot(optionsWithNull)
        );
        assertEquals("Option must not be null", exception.getMessage());
    }
}
