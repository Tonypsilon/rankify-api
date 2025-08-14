package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PollIdTest {

    @Test
    void shouldThrowExceptionForNullUuid() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PollId(null))
                .withMessage("Poll ID must not be null");
    }

    @Test
    void shouldCreatePollIdWithValidUuid() {
        UUID validUuid = UUID.randomUUID();
        PollId pollId = new PollId(validUuid);
        assertThat(pollId.value()).isEqualTo(validUuid);
    }

    @Test
    void shouldCreatePollIdWithRandomUuid() {
        PollId pollId = new PollId();
        assertThat(pollId.value()).isNotNull();
    }
}