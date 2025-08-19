package de.tonypsilon.rankify.api.poll.data;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rankings")
public class Ranking {

    @EmbeddedId
    private RankingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("voteId")
    @JoinColumn(name = "vote_id", nullable = false)
    private VoteEntity vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("optionId")
    @JoinColumn(name = "option_id", nullable = false)
    private OptionEntity option;

    @Column(name = "rank", nullable = false)
    private int rank;

    public Ranking() { /* JPA */ }

    public Ranking(RankingId id, VoteEntity vote, OptionEntity option, int rank) {
        this.id = id;
        this.vote = vote;
        this.option = option;
        this.rank = rank;
    }

    public RankingId getId() {
        return id;
    }

    public void setId(RankingId id) {
        this.id = id;
    }

    public VoteEntity getVote() {
        return vote;
    }

    public void setVote(VoteEntity vote) {
        this.vote = vote;
    }

    public OptionEntity getOption() {
        return option;
    }

    public void setOption(OptionEntity option) {
        this.option = option;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Embeddable
    public static class RankingId implements Serializable {
        @Column(name = "vote_id")
        private UUID voteId;
        @Column(name = "option_id")
        private UUID optionId;

        public RankingId() {
        }

        public RankingId(UUID voteId, UUID optionId) {
            this.voteId = voteId;
            this.optionId = optionId;
        }

        public UUID getVoteId() {
            return voteId;
        }

        public void setVoteId(UUID voteId) {
            this.voteId = voteId;
        }

        public UUID getOptionId() {
            return optionId;
        }

        public void setOptionId(UUID optionId) {
            this.optionId = optionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RankingId that)) return false;
            return Objects.equals(voteId, that.voteId) && Objects.equals(optionId, that.optionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(voteId, optionId);
        }
    }
}
