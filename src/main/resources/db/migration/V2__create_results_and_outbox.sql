CREATE TABLE voting_results (
    voting_session_id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    yes_votes BIGINT NOT NULL,
    no_votes BIGINT NOT NULL,
    outcome VARCHAR(8) NOT NULL,
    closed_at TIMESTAMPTZ NOT NULL,
    finalized_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT voting_results_session_fk
        FOREIGN KEY (voting_session_id) REFERENCES voting_sessions (id),
    CONSTRAINT voting_results_agenda_fk
        FOREIGN KEY (agenda_id) REFERENCES agendas (id),
    CONSTRAINT voting_results_agenda_unique UNIQUE (agenda_id),
    CONSTRAINT voting_results_non_negative_counts
        CHECK (yes_votes >= 0 AND no_votes >= 0),
    CONSTRAINT voting_results_outcome_values
        CHECK (outcome IN ('APPROVED', 'REJECTED', 'TIED')),
    CONSTRAINT voting_results_valid_finalization
        CHECK (finalized_at >= closed_at)
);

CREATE TABLE voting_result_outbox (
    event_id UUID PRIMARY KEY,
    voting_session_id UUID NOT NULL,
    agenda_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    CONSTRAINT voting_result_outbox_result_fk
        FOREIGN KEY (voting_session_id) REFERENCES voting_results (voting_session_id),
    CONSTRAINT voting_result_outbox_agenda_fk
        FOREIGN KEY (agenda_id) REFERENCES agendas (id),
    CONSTRAINT voting_result_outbox_event_unique
        UNIQUE (voting_session_id, event_type),
    CONSTRAINT voting_result_outbox_non_negative_attempts
        CHECK (attempts >= 0),
    CONSTRAINT voting_result_outbox_event_type_not_blank
        CHECK (BTRIM(event_type) <> '')
);

CREATE INDEX voting_result_outbox_pending_idx
    ON voting_result_outbox (next_attempt_at, occurred_at, event_id)
    WHERE published_at IS NULL;
