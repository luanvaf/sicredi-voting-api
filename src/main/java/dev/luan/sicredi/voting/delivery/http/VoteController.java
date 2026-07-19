package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.service.port.in.CastVote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendas/{agendaId}/votes")
@Tag(name = "Votes")
class VoteController {

  private final CastVote castVote;

  VoteController(CastVote castVote) {
    this.castVote = Objects.requireNonNull(castVote, "Cast vote use case must not be null");
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cast an eligible associate's vote")
  @ApiResponse(responseCode = "201", description = "Vote accepted")
  @ApiResponse(responseCode = "409", description = "Session closed or duplicate vote")
  @ApiResponse(responseCode = "422", description = "Associate cannot vote")
  @ApiResponse(responseCode = "503", description = "Eligibility provider unavailable")
  VoteResponse cast(@PathVariable UUID agendaId, @Valid @RequestBody CastVoteRequest request) {
    Vote vote =
        castVote.execute(
            new CastVote.Command(
                new AgendaId(agendaId),
                new AssociateCpf(request.associateCpf()),
                request.choice()));
    return VoteResponse.fromDomain(agendaId, vote);
  }
}
