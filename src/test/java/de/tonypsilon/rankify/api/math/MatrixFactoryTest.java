package de.tonypsilon.rankify.api.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatrixFactoryTest {

    @Test
    void ofRows_builds_backed_matrix_and_reads_values() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(Arrays.asList("a", "b")));
        rows.add(new ArrayList<>(Arrays.asList("c", "d")));

        Matrix<String> m = Matrix.ofRows(rows);

        de.tonypsilon.rankify.api.math.assertions.MatrixAssert.assertThat(m)
                .hasDimensions(2, 2)
                .containsAt(0, 0, "a")
                .containsAt(0, 1, "b")
                .containsAt(1, 0, "c")
                .containsAt(1, 1, "d");

        rows.getFirst().set(1, "x");
        assertThat(m.getElement(new RowIndex(0), new ColumnIndex(1))).isEqualTo("x");
    }

    @Test
    void ofRows_validates_input() {
        List<List<Object>> nullOuter = null;
        List<List<Object>> zeroRows = List.of();
        List<List<Object>> zeroColumns = List.of(List.of());
        List<List<Object>> nonRectangular = List.of(List.of(1, 2), List.of(3));
        List<List<Object>> withNull = new ArrayList<>();
        withNull.add(null);

        assertThatThrownBy(() -> Matrix.ofRows(nullOuter))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Matrix.ofRows(zeroRows))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one row");
        assertThatThrownBy(() -> Matrix.ofRows(zeroColumns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one column");
        assertThatThrownBy(() -> Matrix.ofRows(nonRectangular))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equal length");
        assertThatThrownBy(() -> Matrix.ofRows(withNull))
                .isInstanceOf(NullPointerException.class);
    }
}
