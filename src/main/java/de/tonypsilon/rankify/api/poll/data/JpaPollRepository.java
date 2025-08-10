package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class JpaPollRepository implements PollRepository {

    private final SpringDataPollRepository springDataPollRepository;

    JpaPollRepository(final SpringDataPollRepository springDataPollRepository) {
        this.springDataPollRepository = springDataPollRepository;
    }

    @Override
    public boolean existsById(PollId id) {
        if (id == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }
        return springDataPollRepository.existsById(id.value());
    }

    @Override
    public void update(Poll poll) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PollId create(Poll poll) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Poll getById(PollId pollId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
