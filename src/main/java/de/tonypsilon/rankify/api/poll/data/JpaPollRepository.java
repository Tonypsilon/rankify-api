package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollNotFoundException;
import de.tonypsilon.rankify.api.poll.business.PollRepository;
import de.tonypsilon.rankify.api.poll.business.Vote;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaPollRepository implements PollRepository {

    private final SpringDataPollRepository springDataPollRepository;
    private final PollMapper pollMapper;
    private final JpaVoteRepository voteRepository;
    private final EntityManager entityManager;

    JpaPollRepository(SpringDataPollRepository springDataPollRepository,
                      PollMapper pollMapper,
                      JpaVoteRepository voteRepository,
                      EntityManager entityManager) {
        this.springDataPollRepository = springDataPollRepository;
        this.pollMapper = pollMapper;
        this.voteRepository = voteRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsById(PollId id) {
        if (id == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }
        return springDataPollRepository.existsById(id.value());
    }

    @Override
    @Transactional
    public PollId create(Poll poll) {
        if (poll == null) {
            throw new IllegalArgumentException("Poll must not be null");
        }

        PollEntity entity = pollMapper.toEntity(poll);
        PollEntity saved = springDataPollRepository.save(entity);
        return new PollId(saved.getId());
    }

    @Override
    @Transactional
    public void update(Poll poll) {
        if (poll == null) {
            throw new IllegalArgumentException("Poll must not be null");
        }

        PollEntity existing = springDataPollRepository.findWithOptionsById(poll.id().value())
                .orElseThrow(() -> new PollNotFoundException(poll.id()));

        existing.setTitle(poll.title().value());
        existing.setStart(poll.schedule().start());
        existing.setEnd(poll.schedule().end());
        existing.setCreated(poll.created());

        // Determine if ballot changed (set of option texts)
        var existingTexts = existing.getOptions().stream().map(OptionEntity::getText).sorted().toList();
        var newTexts = poll.ballot().options().stream().map(Option::text).sorted().toList();
        if (!existingTexts.equals(newTexts)) {
            existing.getOptions().clear();
            entityManager.flush();
            poll.ballot().options().forEach(o -> existing.addOption(new OptionEntity(existing, o.text())));
        }
        // else no option modification needed
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Poll getById(PollId pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID must not be null");
        }

        PollEntity entity = springDataPollRepository.findWithOptionsById(pollId.value())
                .orElseThrow(() -> new PollNotFoundException(pollId));

        return pollMapper.toDomain(entity);
    }

    @Override
    public void saveVote(Vote vote) {
        if (vote == null) {
            throw new IllegalArgumentException("Vote must not be null");
        }
        voteRepository.save(vote);
    }
}
