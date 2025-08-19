package de.tonypsilon.rankify.api.poll.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataVoteRepository extends JpaRepository<VoteEntity, UUID> {
}

