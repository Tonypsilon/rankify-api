package de.tonypsilon.rankify.api.math;

import java.util.List;

/**
 * A two-dimensional, index-addressable container of elements.
 * <p>
 * Rows and columns are addressed using the value objects {@link RowIndex} and
 * {@link ColumnIndex}. Index values are zero-based. All matrices are
 * <strong>rectangular</strong>: every row has the same number of columns, and
 * the number of rows and columns is fixed by the backing lists at creation
 * time. A matrix is also <strong>non-empty</strong>: it must have at least one
 * row and at least one column.
 * </p>
 *
 * <p>
 * Unless otherwise noted, all operations are expected to run in time
 * proportional to the backing storage. For the default implementation created
 * via {@link #ofRows(List)}, random access and updates delegate to the provided
 * {@code List} instances.
 * </p>
 *
 * @param <T> the type of elements stored in the matrix
 */
public interface Matrix<T> {

    /**
     * Returns the element at the given row and column.
     *
     * <p>No defensive copying is performed; the returned reference is the value
     * currently stored at the addressed position.</p>
     *
     * @param row    the zero-based row index
     * @param column the zero-based column index
     * @return the element at {@code (row, column)}
     * @throws IndexOutOfBoundsException if {@code row} or {@code column} refer to
     *                                   a position outside the bounds of this
     *                                   rectangular matrix (i.e.,
     *                                   {@code row >= rowCount} or
     *                                   {@code column >= columnCount})
     * @throws NullPointerException      if {@code row} or {@code column} is
     *                                   {@code null}
     * @throws IllegalArgumentException  if either index has a negative value;
     *                                   see {@link RowIndex} and
     *                                   {@link ColumnIndex} for validation rules
     */
    T getElement(RowIndex row, ColumnIndex column);

    /**
     * Replaces the element at the given row and column with the provided value.
     *
     * <p>Whether {@code null} elements are permitted is implementation-dependent.
     * The default implementation created via {@link #ofRows(List)} delegates to
     * {@link java.util.List#set(int, Object)} on the provided row lists.</p>
     *
     * @param row    the zero-based row index
     * @param column the zero-based column index
     * @param value  the element to store at {@code (row, column)}
     * @throws IndexOutOfBoundsException     if {@code row} or {@code column} refer to
     *                                       a position outside the bounds of this
     *                                       rectangular matrix
     * @throws UnsupportedOperationException if the underlying storage does not
     *                                       support element replacement (e.g., an
     *                                       unmodifiable row list)
     * @throws NullPointerException          if {@code row} or {@code column} is
     *                                       {@code null}
     * @throws IllegalArgumentException      if either index has a negative value;
     *                                       see {@link RowIndex} and
     *                                       {@link ColumnIndex}
     */
    void setElement(RowIndex row, ColumnIndex column, T value);

    /**
     * Creates a rectangular, non-empty matrix view backed by the supplied rows.
     *
     * <p>This factory returns a matrix whose storage is the given list of rows;
     * no defensive copies are made. As a result:</p>
     * <ul>
     *     <li>Structural or element changes to {@code rowwiseElements} are
     *     reflected in the returned matrix, and vice versa.</li>
     *     <li>All rows must have the same length; unequal row lengths cause an
     *     {@link IllegalArgumentException}.</li>
     *     <li>The matrix must have at least one row and at least one column;
     *     otherwise an {@link IllegalArgumentException} is thrown.</li>
     *     <li>Mutability of the matrix depends on the mutability of the provided
     *     lists; attempting to set an element may throw
     *     {@link UnsupportedOperationException} if a row list is unmodifiable.</li>
     * </ul>
     *
     * <p>Neither the outer list nor any row list may be {@code null}. Element
     * nullability is not constrained by this factory.</p>
     *
     * @param rowwiseElements rows of elements, indexed by {@link RowIndex} and
     *                        {@link ColumnIndex}
     * @return a matrix backed directly by {@code rowwiseElements}
     * @throws NullPointerException     if {@code rowwiseElements} or any contained
     *                                  row list is {@code null}
     * @throws IllegalArgumentException if the provided rows do not all have the
     *                                  same length, or if there are zero rows, or if
     *                                  any row has zero columns
     */
    static <T> Matrix<T> ofRows(List<List<T>> rowwiseElements) {
        return new SimpleRowBasedMatrix<>(rowwiseElements);
    }

}
