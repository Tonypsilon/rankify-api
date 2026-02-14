package de.tonypsilon.rankify.api.math;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RowIndexTest {

    @Test
    void negativeValue_isRejected() {
        assertThatThrownBy(() -> new RowIndex(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void compareTo_comparesByValue() {
        RowIndex zero = new RowIndex(0);
        RowIndex one = new RowIndex(1);
        assertThat(zero.lessThan(one)).isTrue();
        assertThat(one.lessThan(zero)).isFalse();
        assertThat(zero.greaterThan(one)).isFalse();
        assertThat(one.greaterThan(zero)).isTrue();
        assertThat(zero.lessThan(zero)).isFalse();
        assertThat(zero.greaterThan(zero)).isFalse();
    }
}

