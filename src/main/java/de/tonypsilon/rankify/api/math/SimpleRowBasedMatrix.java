package de.tonypsilon.rankify.api.math;

import java.util.List;
import java.util.Objects;

class SimpleRowBasedMatrix<T> implements Matrix<T> {

    private final List<List<T>> rowwiseElements;
    private final RowIndex maxRowIndex;       // last valid row index (non-null)
    private final ColumnIndex maxColumnIndex; // last valid column index (non-null)

    SimpleRowBasedMatrix(List<List<T>> rowwiseElements) {
        if (Objects.requireNonNull(rowwiseElements).isEmpty()) {
            throw new IllegalArgumentException("matrix must have at least one row");
        }

        final int columnCount = Objects.requireNonNull(rowwiseElements.getFirst()).size();
        if (columnCount == 0) {
            throw new IllegalArgumentException("matrix must have at least one column");
        }

        if (rowwiseElements.stream().anyMatch(r ->
                Objects.requireNonNull(r, "row cannot be null").size() != columnCount)) {
            throw new IllegalArgumentException("All rows must have equal length: expected " + columnCount);
        }

        this.maxRowIndex = new RowIndex(rowwiseElements.size() - 1);
        this.maxColumnIndex = new ColumnIndex(columnCount - 1);
        this.rowwiseElements = rowwiseElements;
    }

    private void assertInBounds(final RowIndex row, final ColumnIndex column) {
        Objects.requireNonNull(row, "row cannot be null");
        Objects.requireNonNull(column, "column cannot be null");
        if (row.compareTo(maxRowIndex) > 0 || column.compareTo(maxColumnIndex) > 0) {
            throw new IndexOutOfBoundsException(
                    "Index (" + row + "," + column + ") out of bounds for max indices (" +
                            maxRowIndex + "," + maxColumnIndex + ")"
            );
        }
    }

    @Override
    public void setElement(final RowIndex row, final ColumnIndex column, final T element) {
        assertInBounds(row, column);
        rowwiseElements.get(row.value()).set(column.value(), element);
    }

    @Override
    public T getElement(final RowIndex row, final ColumnIndex column) {
        assertInBounds(row, column);
        return rowwiseElements.get(row.value()).get(column.value());
    }

}
