package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.PollId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OptionEntityTest {

    @Test
    void getPollIdReturnsNullWhenPollIsNull() {
        OptionEntity entity = new OptionEntity(); // poll unset
        assertThat(entity.getPollId()).isNull();
    }

    @Test
    void getPollIdReturnsWrappedPollIdWhenPollPresent() {
        PollEntity poll = new PollEntity();
        UUID id = UUID.randomUUID();
        poll.setId(id);
        OptionEntity entity = new OptionEntity();
        entity.setPoll(poll);
        assertThat(entity.getPollId()).isEqualTo(new PollId(id));
    }
}

