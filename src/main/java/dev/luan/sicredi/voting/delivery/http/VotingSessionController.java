package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.service.port.in.OpenVotingSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendas/{agendaId}/voting-sessions")
@Tag(name = "Voting sessions")
class VotingSessionController {

  private final OpenVotingSession openVotingSession;

  VotingSessionController(OpenVotingSession openVotingSession) {
    this.openVotingSession =
        Objects.requireNonNull(openVotingSession, "Open voting session use case must not be null");
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Open the voting session for an agenda")
  @ApiResponse(responseCode = "201", description = "Voting session opened")
  @ApiResponse(responseCode = "404", description = "Agenda not found")
  @ApiResponse(responseCode = "409", description = "Agenda already has a voting session")
  VotingSessionResponse open(
      @PathVariable UUID agendaId,
      @Valid @RequestBody(required = false) OpenVotingSessionRequest request) {
    Optional<Duration> requestedDuration =
        Optional.ofNullable(request)
            .map(OpenVotingSessionRequest::durationSeconds)
            .map(Duration::ofSeconds);
    VotingSession votingSession =
        openVotingSession.execute(
            new OpenVotingSession.Command(new AgendaId(agendaId), requestedDuration));
    return VotingSessionResponse.fromDomain(votingSession);
  }
}
