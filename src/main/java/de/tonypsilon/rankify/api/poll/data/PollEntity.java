package de.tonypsilon.rankify.api.poll.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class PollEntity {

    @Id
    private UUID id;
}
