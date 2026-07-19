package dev.luan.sicredi.voting.integration.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgendaRepository extends JpaRepository<AgendaJpaEntity, UUID> {}
