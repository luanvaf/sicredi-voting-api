package dev.luan.sicredi.voting.delivery.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

record OpenVotingSessionRequest(@Min(1) @Max(86_400) Long durationSeconds) {}
