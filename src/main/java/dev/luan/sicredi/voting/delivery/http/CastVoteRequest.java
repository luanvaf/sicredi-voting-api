package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.VoteChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record CastVoteRequest(
    @NotBlank @Pattern(regexp = "\\d{11}") String associateCpf, @NotNull VoteChoice choice) {}
