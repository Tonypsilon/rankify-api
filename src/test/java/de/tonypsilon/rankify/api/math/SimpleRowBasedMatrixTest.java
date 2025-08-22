package de.tonypsilon.rankify.api.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleRowBasedMatrixTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {
        @Test
        void rejects_null_outer_list() {
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_zero_rows() {
            List<List<Integer>> zeroRows = List.of();
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(zeroRows))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one row");
        }

        @Test
        void rejects_null_first_row() {
            List<List<Integer>> rows = new ArrayList<>();
            rows.add(null);
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(rows))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_zero_columns() {
            List<List<Integer>> zeroColumns = List.of(List.of());
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(zeroColumns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one column");
        }

        @Test
        void rejects_any_null_row() {
            List<List<Integer>> rows = new ArrayList<>();
            rows.add(new ArrayList<>(List.of(1)));
            rows.add(null);
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(rows))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("row cannot be null");
        }

        @Test
        void rejects_non_rectangular() {
            List<List<Integer>> nonRectangular = List.of(List.of(1, 2), List.of(3));
            assertThatThrownBy(() -> new SimpleRowBasedMatrix<>(nonRectangular))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("equal length");
        }
    }

    @Nested
    @DisplayName("Element access and update")
    class ElementAccessAndUpdate {
        @Test
        void getElement_returns_expected_values() {
            List<List<String>> rows = new ArrayList<>();
            rows.add(new ArrayList<>(Arrays.asList("a", "b")));
            rows.add(new ArrayList<>(Arrays.asList("c", "d")));
            SimpleRowBasedMatrix<String> m = new SimpleRowBasedMatrix<>(rows);

            assertThat(m.getElement(new RowIndex(0), new ColumnIndex(0))).isEqualTo("a");
            assertThat(m.getElement(new RowIndex(0), new ColumnIndex(1))).isEqualTo("b");
            assertThat(m.getElement(new RowIndex(1), new ColumnIndex(0))).isEqualTo("c");
            assertThat(m.getElement(new RowIndex(1), new ColumnIndex(1))).isEqualTo("d");
        }

        @Test
        void setElement_updates_backing_list() {
            List<List<Integer>> rows = new ArrayList<>();
            rows.add(new ArrayList<>(Arrays.asList(1, 2)));
            rows.add(new ArrayList<>(Arrays.asList(3, 4)));
            SimpleRowBasedMatrix<Integer> m = new SimpleRowBasedMatrix<>(rows);

            m.setElement(new RowIndex(0), new ColumnIndex(1), 42);

            assertThat(rows.get(0)).containsExactly(1, 42);
            assertThat(m.getElement(new RowIndex(0), new ColumnIndex(1))).isEqualTo(42);
        }

        @Test
        void null_indices_are_rejected() {
            List<List<Integer>> rows = new ArrayList<>();
            rows.add(new ArrayList<>(List.of(7)));
            SimpleRowBasedMatrix<Integer> m = new SimpleRowBasedMatrix<>(rows);

            ColumnIndex col0 = new ColumnIndex(0);
            RowIndex row0 = new RowIndex(0);

            assertThatThrownBy(() -> m.getElement(null, col0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("row cannot be null");
            assertThatThrownBy(() -> m.getElement(row0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("column cannot be null");

            assertThatThrownBy(() -> m.setElement(null, col0, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("row cannot be null");
            assertThatThrownBy(() -> m.setElement(row0, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("column cannot be null");
        }

        @Test
        void out_of_bounds_indices_throw_IndexOutOfBoundsException() {
            List<List<Integer>> rows = new ArrayList<>();
            rows.add(new ArrayList<>(Arrays.asList(1, 2)));
            rows.add(new ArrayList<>(Arrays.asList(3, 4)));
            SimpleRowBasedMatrix<Integer> m = new SimpleRowBasedMatrix<>(rows);

            RowIndex row2 = new RowIndex(2);
            ColumnIndex col0 = new ColumnIndex(0);
            RowIndex row0 = new RowIndex(0);
            ColumnIndex col2 = new ColumnIndex(2);

            assertThatThrownBy(() -> m.getElement(row2, col0))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> m.getElement(row0, col2))
                    .isInstanceOf(IndexOutOfBoundsException.class);

            assertThatThrownBy(() -> m.setElement(row2, col0, 0))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> m.setElement(row0, col2, 0))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }
}
