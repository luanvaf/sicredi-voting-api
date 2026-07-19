package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.service.port.in.CreateAgenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendas")
@Tag(name = "Agendas")
class AgendaController {

  private final CreateAgenda createAgenda;

  AgendaController(CreateAgenda createAgenda) {
    this.createAgenda =
        Objects.requireNonNull(createAgenda, "Create agenda use case must not be null");
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new voting agenda")
  @ApiResponse(responseCode = "201", description = "Agenda created")
  @ApiResponse(responseCode = "400", description = "Invalid agenda data")
  AgendaResponse create(@Valid @RequestBody CreateAgendaRequest request) {
    Agenda agenda =
        createAgenda.execute(new CreateAgenda.Command(request.title(), request.description()));
    return AgendaResponse.fromDomain(agenda);
  }
}
