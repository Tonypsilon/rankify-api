package de.tonypsilon.rankify.api.poll.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "polls")
public class PollEntity {

    @Id
    private UUID id;

    @Column
    private String title;

    @Column
    private LocalDateTime start;

    @Column
    private LocalDateTime end;

    @Column
    private LocalDateTime created;
}

