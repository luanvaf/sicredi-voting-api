package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateAgendaRequest(
    @NotBlank @Size(max = Agenda.MAXIMUM_TITLE_LENGTH) String title,
    @Size(max = Agenda.MAXIMUM_DESCRIPTION_LENGTH) String description) {}
