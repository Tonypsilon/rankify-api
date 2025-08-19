package de.tonypsilon.rankify.api.poll.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataOptionRepository extends JpaRepository<OptionEntity, UUID> {

    Optional<OptionEntity> findByPoll_IdAndText(UUID pollId, String text);
}
