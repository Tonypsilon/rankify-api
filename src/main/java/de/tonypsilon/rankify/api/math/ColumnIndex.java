package de.tonypsilon.rankify.api.math;

public record ColumnIndex(int value) implements Index<ColumnIndex> {

    public ColumnIndex {
        if (value < 0) throw new IllegalArgumentException("value cannot be negative");
    }

}
