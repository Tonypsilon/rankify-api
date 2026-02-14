package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Vote;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
class JpaVoteRepository {

    private final SpringDataVoteRepository springDataVoteRepository;
    private final SpringDataOptionRepository optionRepository;

    JpaVoteRepository(SpringDataVoteRepository springDataVoteRepository,
                      SpringDataOptionRepository optionRepository) {
        this.springDataVoteRepository = springDataVoteRepository;
        this.optionRepository = optionRepository;
    }

    public void save(Vote vote) {
        if (vote == null) {
            throw new IllegalArgumentException("Vote must not be null");
        }
        UUID pollId = vote.pollId().value();
        if (pollId == null) {
            throw new IllegalArgumentException("Vote.pollId must not be null");
        }

        VoteEntity voteEntity = new VoteEntity();
        voteEntity.setId(UUID.randomUUID());
        voteEntity.setPollId(pollId);
        voteEntity.setSubmittedAt(Instant.now());

        for (Map.Entry<Option, Integer> e : vote.rankings().entrySet()) {
            Option option = e.getKey();
            Integer rank = e.getValue();
            if (option == null || rank == null) {
                throw new IllegalArgumentException("Option and rank must not be null");
            }
            OptionEntity optionEntity = optionRepository.findByPoll_IdAndText(pollId, option.text())
                    .orElseThrow(() -> new IllegalArgumentException("Option not found for poll: " + option.text()));
            Ranking.RankingId rankingId = new Ranking.RankingId(voteEntity.getId(), optionEntity.getId());
            Ranking ranking = new Ranking(rankingId, voteEntity, optionEntity, rank);
            voteEntity.addRanking(ranking);
        }

        springDataVoteRepository.save(voteEntity);
    }
}
