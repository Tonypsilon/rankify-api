package de.tonypsilon.rankify.api.math;

public record ColumnIndex(int value) implements Comparable<ColumnIndex> {

    public ColumnIndex {
        if (value < 0) throw new IllegalArgumentException("value cannot be negative");
    }

    @Override
    public int compareTo(ColumnIndex other) {
        return Integer.compare(value, other.value);
    }
}
