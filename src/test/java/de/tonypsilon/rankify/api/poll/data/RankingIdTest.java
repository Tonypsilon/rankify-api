package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.data.Ranking.RankingId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RankingIdTest {

    @Test
    void equalsSameInstance() {
        RankingId id = new RankingId(UUID.randomUUID(), UUID.randomUUID());
        assertThat(id).isEqualTo(id);
    }

    @Test
    void equalsSameValues() {
        UUID vote = UUID.randomUUID();
        UUID option = UUID.randomUUID();
        RankingId id1 = new RankingId(vote, option);
        RankingId id2 = new RankingId(vote, option);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void notEqualsDifferentVoteId() {
        UUID option = UUID.randomUUID();
        RankingId id1 = new RankingId(UUID.randomUUID(), option);
        RankingId id2 = new RankingId(UUID.randomUUID(), option);
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void notEqualsDifferentOptionId() {
        UUID vote = UUID.randomUUID();
        RankingId id1 = new RankingId(vote, UUID.randomUUID());
        RankingId id2 = new RankingId(vote, UUID.randomUUID());
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void notEqualsNull() {
        RankingId id = new RankingId(UUID.randomUUID(), UUID.randomUUID());
        assertThat(id).isNotEqualTo(null);
    }

    @Test
    void notEqualsDifferentType() {
        RankingId id = new RankingId(UUID.randomUUID(), UUID.randomUUID());
        assertThat(id).isNotEqualTo("someString");
    }
}

