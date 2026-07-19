package dev.luan.sicredi.voting.integration.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataVotingSessionRepository extends JpaRepository<VotingSessionJpaEntity, UUID> {

  Optional<VotingSessionJpaEntity> findByAgendaId(UUID agendaId);
}
