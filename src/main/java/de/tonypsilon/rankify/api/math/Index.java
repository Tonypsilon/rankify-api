package de.tonypsilon.rankify.api.math;

public interface Index<T extends Index<T>> {

    int value();

    default boolean greaterThan(T other) {
        return value() > other.value();
    }

    default boolean lessThan(T other) {
        return value() < other.value();
    }
}
