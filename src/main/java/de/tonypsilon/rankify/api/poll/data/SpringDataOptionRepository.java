package de.tonypsilon.rankify.api.poll.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataOptionRepository extends JpaRepository<OptionEntity, OptionEntity.OptionId> {

    List<OptionEntity> findByIdPollIdOrderByIdText(UUID pollId);

    void deleteByIdPollId(UUID pollId);
}
