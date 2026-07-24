-- Collaborative estimation sessions (phase-11, task-063). A moderator starts a
-- session against an estimation's DRAFT version, over a chosen subset of the
-- draft's leaf items, and walks each item through a two-phase Wideband-Delphi
-- flow. This migration adds the persistence layer only (REST/WebSocket land in
-- task-064/065). The vote/finalize columns are the THREE_POINT_PERT shape
-- (min/expected/max); the bucket+sampled method votes differently and extends
-- storage separately (task-106).
--
-- Every table carries created_at/updated_at (JPA BaseEntity). Participant join
-- time and vote submission time ARE their created_at — no separate column.

CREATE TABLE estimation_sessions (
    id UUID PRIMARY KEY,
    estimation_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    moderator_subject_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    current_item_index INT NOT NULL DEFAULT 0,
    current_phase VARCHAR(255) NOT NULL,
    finalized_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_estimation_sessions_estimation FOREIGN KEY (estimation_id)
        REFERENCES estimations(id) ON DELETE CASCADE
);

CREATE INDEX idx_estimation_sessions_estimation ON estimation_sessions(estimation_id);

CREATE TABLE session_items (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    node_logical_id VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    discussion_notes TEXT NULL,
    final_min_effort DOUBLE PRECISION NULL,
    final_expected_effort DOUBLE PRECISION NULL,
    final_max_effort DOUBLE PRECISION NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_session_items_session FOREIGN KEY (session_id)
        REFERENCES estimation_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uq_session_items_node UNIQUE (session_id, node_logical_id)
);

CREATE INDEX idx_session_items_session ON session_items(session_id);

CREATE TABLE session_participants (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    subject_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    participant_role VARCHAR(255) NOT NULL,
    agreed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_session_participants_session FOREIGN KEY (session_id)
        REFERENCES estimation_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uq_session_participants_subject UNIQUE (session_id, subject_id)
);

CREATE INDEX idx_session_participants_session ON session_participants(session_id);

CREATE TABLE session_votes (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    session_item_id UUID NOT NULL,
    participant_subject_id VARCHAR(255) NOT NULL,
    phase VARCHAR(255) NOT NULL,
    min_effort DOUBLE PRECISION NOT NULL,
    expected_effort DOUBLE PRECISION NOT NULL,
    max_effort DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_session_votes_session FOREIGN KEY (session_id)
        REFERENCES estimation_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_votes_item FOREIGN KEY (session_item_id)
        REFERENCES session_items(id) ON DELETE CASCADE,
    CONSTRAINT uq_session_votes_item_participant_phase UNIQUE (session_item_id, participant_subject_id, phase)
);

CREATE INDEX idx_session_votes_session ON session_votes(session_id);
CREATE INDEX idx_session_votes_item ON session_votes(session_item_id);
