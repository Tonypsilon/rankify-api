package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaPollRepository extends PollRepository, JpaRepository<PollEntity, UUID> {

    @Override
    default boolean existsById(PollId id) {
        if (id == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }
        return existsById(id.value());
    }

    @Override
    default void update(Poll poll) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    default PollId create(Poll poll) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    default Poll getById(PollId pollId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
