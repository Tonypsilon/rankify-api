package de.tonypsilon.rankify.api.math;

public record RowIndex(int value) implements Comparable<RowIndex> {

    public RowIndex {
        if (value < 0) throw new IllegalArgumentException("value cannot be negative");
    }

    @Override
    public int compareTo(RowIndex other) {
        return Integer.compare(value, other.value);
    }
}
