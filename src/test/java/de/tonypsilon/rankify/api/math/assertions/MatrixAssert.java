package de.tonypsilon.rankify.api.math.assertions;

import de.tonypsilon.rankify.api.math.ColumnIndex;
import de.tonypsilon.rankify.api.math.Matrix;
import de.tonypsilon.rankify.api.math.RowIndex;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MatrixAssert<T> extends AbstractAssert<MatrixAssert<T>, Matrix<T>> {

    public MatrixAssert(Matrix<T> actual) {
        super(actual, MatrixAssert.class);
    }

    public static <T> MatrixAssert<T> assertThat(Matrix<T> actual) {
        return new MatrixAssert<>(actual);
    }

    public MatrixAssert<T> hasDimensions(int expectedRows, int expectedColumns) {
        isNotNull();
        if (expectedRows <= 0 || expectedColumns <= 0) {
            failWithMessage("Expected positive dimensions but got %dx%d", expectedRows, expectedColumns);
        }
        // In-bounds read of every cell should not throw
        for (int r = 0; r < expectedRows; r++) {
            for (int c = 0; c < expectedColumns; c++) {
                RowIndex rr = new RowIndex(r);
                ColumnIndex cc = new ColumnIndex(c);
                assertThatCode(() -> actual.getElement(rr, cc))
                        .as("element at (%d,%d) should be readable", r, c)
                        .doesNotThrowAnyException();
            }
        }
        // Out-of-bounds checks using preconstructed indices
        RowIndex badRow = new RowIndex(expectedRows);
        ColumnIndex zeroCol = new ColumnIndex(0);
        assertThatThrownBy(() -> actual.getElement(badRow, zeroCol))
                .as("row index %d should be out of bounds", badRow.value())
                .isInstanceOf(IndexOutOfBoundsException.class);

        ColumnIndex badCol = new ColumnIndex(expectedColumns);
        RowIndex zeroRow = new RowIndex(0);
        assertThatThrownBy(() -> actual.getElement(zeroRow, badCol))
                .as("column index %d should be out of bounds", badCol.value())
                .isInstanceOf(IndexOutOfBoundsException.class);

        return this;
    }

    public MatrixAssert<T> containsAt(int row, int column, T expected) {
        isNotNull();
        RowIndex rr = new RowIndex(row);
        ColumnIndex cc = new ColumnIndex(column);
        org.assertj.core.api.Assertions.assertThat(actual.getElement(rr, cc))
                .as("element at (%d,%d)", row, column)
                .isEqualTo(expected);
        return this;
    }
}
