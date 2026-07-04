-- Undo system persistence (task-073): a revision counter plus audit columns on
-- the draft version, and an append-only per-draft mutation log. All timestamp
-- columns use plain TIMESTAMP (without time zone) to match the existing schema
-- and the Instant mappings that already pass Hibernate `validate`.

ALTER TABLE draft_estimation_versions ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE draft_estimation_versions ADD COLUMN last_modified_by UUID NULL REFERENCES users(id);
ALTER TABLE draft_estimation_versions ADD COLUMN last_modified_at TIMESTAMP NULL;

CREATE TABLE draft_mutation_log (
    id UUID PRIMARY KEY,
    draft_version_id UUID NOT NULL,
    user_id UUID NOT NULL,
    sequence_number BIGINT NOT NULL,
    revision_before BIGINT NOT NULL,
    revision_after BIGINT NOT NULL,
    kind VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    inverse_payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    undone_at TIMESTAMP NULL,
    CONSTRAINT fk_draft_mutation_log_version FOREIGN KEY (draft_version_id)
        REFERENCES draft_estimation_versions(id) ON DELETE CASCADE,
    CONSTRAINT fk_draft_mutation_log_user FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT uq_draft_mutation_log_seq UNIQUE (draft_version_id, sequence_number)
);

CREATE INDEX idx_draft_mutation_log_latest
    ON draft_mutation_log(draft_version_id, status, sequence_number DESC);
CREATE INDEX idx_draft_mutation_log_user_latest
    ON draft_mutation_log(draft_version_id, user_id, status, sequence_number DESC);
