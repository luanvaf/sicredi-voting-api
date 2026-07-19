CREATE TABLE agendas (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT agendas_title_not_blank CHECK (BTRIM(title) <> '')
);

CREATE TABLE voting_sessions (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    finalized_at TIMESTAMPTZ,
    CONSTRAINT voting_sessions_agenda_fk
        FOREIGN KEY (agenda_id) REFERENCES agendas (id),
    CONSTRAINT voting_sessions_agenda_unique UNIQUE (agenda_id),
    CONSTRAINT voting_sessions_valid_window CHECK (closes_at > opened_at),
    CONSTRAINT voting_sessions_valid_finalization
        CHECK (finalized_at IS NULL OR finalized_at >= closes_at)
);

CREATE INDEX voting_sessions_pending_closure_idx
    ON voting_sessions (closes_at, id)
    WHERE finalized_at IS NULL;

CREATE TABLE votes (
    id UUID PRIMARY KEY,
    voting_session_id UUID NOT NULL,
    associate_cpf VARCHAR(11) NOT NULL,
    choice VARCHAR(3) NOT NULL,
    cast_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT votes_session_fk
        FOREIGN KEY (voting_session_id) REFERENCES voting_sessions (id),
    CONSTRAINT votes_associate_once_per_session
        UNIQUE (voting_session_id, associate_cpf),
    CONSTRAINT votes_associate_cpf_format
        CHECK (associate_cpf ~ '^[0-9]{11}$'),
    CONSTRAINT votes_choice_values
        CHECK (choice IN ('YES', 'NO'))
);

CREATE INDEX votes_session_choice_idx
    ON votes (voting_session_id, choice);
