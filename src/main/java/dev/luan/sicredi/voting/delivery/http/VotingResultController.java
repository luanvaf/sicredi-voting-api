package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.service.port.in.GetVotingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendas/{agendaId}/result")
@Tag(name = "Voting results")
class VotingResultController {

  private final GetVotingResult getVotingResult;

  VotingResultController(GetVotingResult getVotingResult) {
    this.getVotingResult =
        Objects.requireNonNull(getVotingResult, "Get voting result use case must not be null");
  }

  @GetMapping
  @Operation(summary = "Get the finalized result for an agenda")
  @ApiResponse(responseCode = "200", description = "Finalized result returned")
  @ApiResponse(responseCode = "404", description = "Agenda not found")
  @ApiResponse(responseCode = "409", description = "Result is not finalized yet")
  VotingResultResponse get(@PathVariable UUID agendaId) {
    VotingResult result = getVotingResult.execute(new AgendaId(agendaId));
    return VotingResultResponse.fromDomain(result);
  }
}
