package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class OptionTest {

    @Test
    void shouldThrowExceptionForNullText() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Option(null))
                .withMessage("Option text must not be null or blank");
    }

    @Test
    void shouldThrowExceptionForBlankText() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Option(" "))
                .withMessage("Option text must not be null or blank");
    }

    @Test
    void shouldCreateOptionWithValidText() {
        String validText = "Valid Option";
        Option option = new Option(validText);
        assertThat(option.text()).isEqualTo(validText);
    }

}