package de.tonypsilon.rankify.api.poll.business;

import jakarta.annotation.Nonnull;

/**
 * Repository interface for managing Poll entities.
 * Provides methods to create, update, check existence, and retrieve polls by their ID.
 */
public interface PollRepository {

    /**
     * Saves a new poll to the repository.
     *
     * @param poll the Poll object to create
     * @return the ID of the created poll
     * @throws IllegalArgumentException if the poll is null
     * @throws IllegalArgumentException if the poll's ID is null
     * @throws IllegalArgumentException if the poll's title is null
     * @throws IllegalArgumentException if the poll's ballot is null
     * @throws IllegalArgumentException if the poll's schedule is null
     */
    PollId create(Poll poll);

    /**
     * Updates an existing poll in the repository.
     *
     * @param poll the Poll object to update
     * @throws IllegalArgumentException if the poll is null
     * @throws IllegalArgumentException if the poll's ID is null
     * @throws PollNotFoundException    if no poll with the given ID exists
     * @throws IllegalArgumentException if the poll's title has changed
     * @throws IllegalArgumentException if the poll's ballot has changed
     */
    void update(Poll poll);

    /**
     * Checks if a poll with the given ID exists.
     *
     * @param pollId the ID of the poll to check
     * @return true if a poll with the given ID exists, false otherwise
     * @throws IllegalArgumentException if the pollId is null
     */
    boolean existsById(PollId pollId);

    /**
     * Retrieves a poll by its ID.
     *
     * @param pollId the ID of the poll to retrieve
     * @return the Poll object associated with the given ID
     * @throws IllegalArgumentException if the pollId is null
     * @throws PollNotFoundException    if no poll with the given ID exists
     */
    @Nonnull
    Poll getById(PollId pollId);

    void saveVote(Vote vote);
}
