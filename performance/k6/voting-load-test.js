import execution from "k6/execution";
import http from "k6/http";
import { check, fail } from "k6";
import { Rate, Trend } from "k6/metrics";

const SMOKE_MODE = readBoolean("SMOKE", false);
const BASE_URL = readBaseUrl(__ENV.BASE_URL || "http://localhost:8080");
const TOTAL_VOTES = readPositiveInteger(
  "TOTAL_VOTES",
  SMOKE_MODE ? 10 : 100_000,
);
const VIRTUAL_USERS = readPositiveInteger("VUS", SMOKE_MODE ? 1 : 200);
const MAX_DURATION = __ENV.MAX_DURATION || (SMOKE_MODE ? "1m" : "15m");
const ELIGIBILITY_MODE = readEligibilityMode(
  __ENV.ELIGIBILITY_MODE || "worker",
);
const VOTE_CHOICE = readVoteChoice(__ENV.VOTE_CHOICE || "ALTERNATE");

const CPF_SEED = 100_000_000;
const CPF_SEARCH_BLOCK_SIZE = 32;
const MAX_CPF_BASE = 999_999_999;

assertCpfCapacity(TOTAL_VOTES);

const voteDuration = new Trend("vote_duration", true);
const voteFailures = new Rate("vote_failures");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    votes: {
      executor: "shared-iterations",
      vus: VIRTUAL_USERS,
      iterations: TOTAL_VOTES,
      maxDuration: MAX_DURATION,
      gracefulStop: "15s",
    },
  },
  thresholds: {
    checks: ["rate>0.99"],
    dropped_iterations: ["count<1"],
    http_req_failed: ["rate<0.01"],
    iterations: [`count>=${TOTAL_VOTES}`],
    "http_req_duration{endpoint:vote}": ["p(95)<1000", "p(99)<2000"],
    vote_duration: ["p(95)<1000", "p(99)<2000"],
    vote_failures: ["rate<0.01"],
  },
};

export function setup() {
  const agendaResponse = http.post(
    `${BASE_URL}/api/v1/agendas`,
    JSON.stringify({
      title: `k6 performance run ${new Date().toISOString()}`,
      description: `${TOTAL_VOTES} deterministic unique votes`,
    }),
    requestParameters("agenda", true),
  );

  const agendaCreated = check(agendaResponse, {
    "agenda creation returns 201": (response) => response.status === 201,
  });
  if (!agendaCreated) {
    fail(
      `Could not create the performance-test agenda: HTTP ${agendaResponse.status} ${agendaResponse.body}`,
    );
  }

  const agendaId = agendaResponse.json("id");
  if (!agendaId) {
    fail("Agenda creation response does not contain an id");
  }

  const sessionResponse = http.post(
    `${BASE_URL}/api/v1/agendas/${agendaId}/voting-sessions`,
    JSON.stringify({ durationSeconds: 86_400 }),
    requestParameters("session", true),
  );

  const sessionCreated = check(sessionResponse, {
    "session creation returns 201": (response) => response.status === 201,
  });
  if (!sessionCreated) {
    fail(
      `Could not open the performance-test session: HTTP ${sessionResponse.status} ${sessionResponse.body}`,
    );
  }

  return { agendaId };
}

export default function castUniqueVote(data) {
  const iteration = execution.scenario.iterationInTest;
  const associateCpf = cpfForIteration(iteration, ELIGIBILITY_MODE);
  const choice = choiceForIteration(iteration, VOTE_CHOICE);

  const response = http.post(
    `${BASE_URL}/api/v1/agendas/${data.agendaId}/votes`,
    JSON.stringify({ associateCpf, choice }),
    requestParameters("vote", false),
  );

  const accepted = check(response, {
    "vote creation returns 201": (result) => result.status === 201,
  });
  voteDuration.add(response.timings.duration);
  voteFailures.add(!accepted);
}

export function cpfForIteration(iteration, eligibilityMode = "worker") {
  const blockStart = CPF_SEED + iteration * CPF_SEARCH_BLOCK_SIZE;

  for (let offset = 0; offset < CPF_SEARCH_BLOCK_SIZE; offset += 1) {
    const cpf = buildCpf(blockStart + offset);
    const workerAcceptsCpf = Number(cpf[10]) % 2 === 0;

    if (eligibilityMode === "always" || workerAcceptsCpf) {
      return cpf;
    }
  }

  fail(
    `Could not generate an eligible CPF for iteration ${iteration}; increase CPF_SEARCH_BLOCK_SIZE`,
  );
}

function buildCpf(baseNumber) {
  const base = String(baseNumber).padStart(9, "0");
  const firstCheckDigit = calculateCheckDigit(base, 9, 10);
  const secondCheckDigit = calculateCheckDigit(
    `${base}${firstCheckDigit}`,
    10,
    11,
  );
  return `${base}${firstCheckDigit}${secondCheckDigit}`;
}

function calculateCheckDigit(value, digitCount, initialWeight) {
  let sum = 0;
  for (let index = 0; index < digitCount; index += 1) {
    sum += Number(value[index]) * (initialWeight - index);
  }

  const digit = 11 - (sum % 11);
  return digit >= 10 ? 0 : digit;
}

function choiceForIteration(iteration, configuredChoice) {
  if (configuredChoice === "ALTERNATE") {
    return iteration % 2 === 0 ? "YES" : "NO";
  }
  return configuredChoice;
}

function requestParameters(endpoint, retainResponseBody) {
  return {
    headers: { "Content-Type": "application/json" },
    responseType: retainResponseBody ? "text" : "none",
    tags: { endpoint },
    timeout: "10s",
  };
}

function readBaseUrl(value) {
  const normalized = value.replace(/\/+$/, "");
  if (!/^https?:\/\/[^/]+/i.test(normalized)) {
    throw new Error(`BASE_URL must be an HTTP(S) URL, received "${value}"`);
  }
  return normalized;
}

function readPositiveInteger(name, fallback) {
  const rawValue = __ENV[name];
  if (rawValue === undefined || rawValue === "") {
    return fallback;
  }

  const parsed = Number(rawValue);
  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer, received "${rawValue}"`);
  }
  return parsed;
}

function readBoolean(name, fallback) {
  const rawValue = __ENV[name];
  if (rawValue === undefined || rawValue === "") {
    return fallback;
  }

  if (rawValue === "true") {
    return true;
  }
  if (rawValue === "false") {
    return false;
  }
  throw new Error(`${name} must be "true" or "false", received "${rawValue}"`);
}

function readEligibilityMode(value) {
  const normalized = value.toLowerCase();
  if (normalized !== "worker" && normalized !== "always") {
    throw new Error(
      `ELIGIBILITY_MODE must be "worker" or "always", received "${value}"`,
    );
  }
  return normalized;
}

function readVoteChoice(value) {
  const normalized = value.toUpperCase();
  if (!["YES", "NO", "ALTERNATE"].includes(normalized)) {
    throw new Error(
      `VOTE_CHOICE must be "YES", "NO", or "ALTERNATE", received "${value}"`,
    );
  }
  return normalized;
}

function assertCpfCapacity(totalVotes) {
  const highestBase =
    CPF_SEED + (totalVotes - 1) * CPF_SEARCH_BLOCK_SIZE +
    CPF_SEARCH_BLOCK_SIZE -
    1;
  if (highestBase > MAX_CPF_BASE) {
    const maximumVotes = Math.floor(
      (MAX_CPF_BASE - CPF_SEED + 1) / CPF_SEARCH_BLOCK_SIZE,
    );
    throw new Error(
      `TOTAL_VOTES exceeds the deterministic CPF capacity of ${maximumVotes}`,
    );
  }
}
