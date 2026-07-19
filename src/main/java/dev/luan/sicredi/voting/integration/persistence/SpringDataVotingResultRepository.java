package dev.luan.sicredi.voting.integration.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataVotingResultRepository extends JpaRepository<VotingResultJpaEntity, UUID> {

  Optional<VotingResultJpaEntity> findByAgendaId(UUID agendaId);
}
