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
        RowIndex r0 = new RowIndex(0);
        RowIndex r1 = new RowIndex(1);
        assertThat(r0.compareTo(r1)).isLessThan(0);
        assertThat(r1.compareTo(r0)).isGreaterThan(0);
        assertThat(r0.compareTo(new RowIndex(0))).isZero();
    }
}

