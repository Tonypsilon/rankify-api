package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollNotFoundException;
import de.tonypsilon.rankify.api.poll.business.PollRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JpaPollRepository implements PollRepository {

    private final SpringDataPollRepository springDataPollRepository;
    private final PollMapper pollMapper;
    private final SpringDataOptionRepository optionRepository;

    JpaPollRepository(SpringDataPollRepository springDataPollRepository,
                     PollMapper pollMapper,
                     SpringDataOptionRepository optionRepository) {
        this.springDataPollRepository = springDataPollRepository;
        this.pollMapper = pollMapper;
        this.optionRepository = optionRepository;
    }

    @Override
    public boolean existsById(PollId id) {
        if (id == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }
        return springDataPollRepository.existsById(id.value());
    }

    @Override
    public PollId create(Poll poll) {
        if (poll == null) {
            throw new IllegalArgumentException("Poll must not be null");
        }

        PollEntity entity = pollMapper.toEntity(poll);
        PollEntity savedEntity = springDataPollRepository.save(entity);

        List<OptionEntity> optionEntities = pollMapper.toOptionEntities(poll.id(), poll.ballot());
        optionRepository.saveAll(optionEntities);

        return new PollId(savedEntity.getId());
    }

    @Override
    public void update(Poll poll) {
        if (poll == null) {
            throw new IllegalArgumentException("Poll must not be null");
        }

        PollEntity entity = pollMapper.toEntity(poll);
        springDataPollRepository.save(entity);

        // Update options: delete existing and save new ones
        optionRepository.deleteByIdPollId(poll.id().value());
        List<OptionEntity> optionEntities = pollMapper.toOptionEntities(poll.id(), poll.ballot());
        optionRepository.saveAll(optionEntities);
    }

    @Override
    public Poll getById(PollId pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }

        PollEntity entity = springDataPollRepository.findById(pollId.value())
                .orElseThrow(() -> new PollNotFoundException(pollId));

        List<OptionEntity> optionEntities = optionRepository.findByIdPollIdOrderByIdText(pollId.value());

        return pollMapper.toDomain(entity, optionEntities);
    }
}
