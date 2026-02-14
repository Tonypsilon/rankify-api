package de.tonypsilon.rankify.api.math;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColumnIndexTest {

    @Test
    void negativeValue_isRejected() {
        assertThatThrownBy(() -> new ColumnIndex(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void compareTo_comparesByValue() {
        ColumnIndex zero = new ColumnIndex(0);
        ColumnIndex one = new ColumnIndex(1);
        assertThat(zero.lessThan(one)).isTrue();
        assertThat(one.lessThan(zero)).isFalse();
        assertThat(zero.greaterThan(one)).isFalse();
        assertThat(one.greaterThan(zero)).isTrue();
        assertThat(zero.lessThan(zero)).isFalse();
        assertThat(zero.greaterThan(zero)).isFalse();
    }
}

