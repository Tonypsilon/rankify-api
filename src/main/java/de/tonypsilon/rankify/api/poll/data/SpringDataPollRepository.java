package de.tonypsilon.rankify.api.poll.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPollRepository extends JpaRepository<PollEntity, UUID> {

    @Query("select distinct p from PollEntity p left join fetch p.options where p.id = :id")
    Optional<PollEntity> findWithOptionsById(@Param("id") UUID id);
}
