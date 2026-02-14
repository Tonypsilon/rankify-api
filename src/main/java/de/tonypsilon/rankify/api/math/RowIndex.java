package de.tonypsilon.rankify.api.math;

public record RowIndex(int value) implements Index<RowIndex> {

    public RowIndex {
        if (value < 0) throw new IllegalArgumentException("value cannot be negative");
    }

}
