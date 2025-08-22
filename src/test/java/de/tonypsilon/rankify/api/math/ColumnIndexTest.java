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
        ColumnIndex c0 = new ColumnIndex(0);
        ColumnIndex c1 = new ColumnIndex(1);
        assertThat(c0.compareTo(c1)).isLessThan(0);
        assertThat(c1.compareTo(c0)).isGreaterThan(0);
        assertThat(c0.compareTo(new ColumnIndex(0))).isZero();
    }
}

