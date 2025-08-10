package de.tonypsilon.rankify.api.poll.business;

import java.time.LocalDateTime;

/**
 * Represents the schedule of a poll, including its start and end times.
 * Both times can be null, but if both are provided:
 * The end time must not be before the start time.
 */
public record Schedule(LocalDateTime start, LocalDateTime end) {
    public Schedule {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Poll end time cannot be before start time");
        }
    }

    public Schedule withStart(final LocalDateTime start) {
        return new Schedule(start, this.end);
    }

    public Schedule withEnd(final LocalDateTime end) {
        return new Schedule(this.start, end);
    }
}
