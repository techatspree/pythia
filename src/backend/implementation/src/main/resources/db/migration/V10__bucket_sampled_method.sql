-- Bucket + sampled estimation method (task-103). Buckets are owned by the
-- estimation (shared across its draft + submitted versions); bucketed leaf rows
-- on both node tables reference a bucket and flag whether they are a sample.
-- A sample stores its optimistic/likely/pessimistic triple in the existing
-- min_effort/expected_effort/max_effort columns; non-samples leave them NULL and
-- inherit the bucket average via EstimationVersion.calculate().

CREATE TABLE estimation_buckets (
    id UUID PRIMARY KEY,
    estimation_id UUID NOT NULL,
    position INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_estimation_buckets_estimation FOREIGN KEY (estimation_id)
        REFERENCES estimations(id) ON DELETE CASCADE,
    CONSTRAINT uq_estimation_buckets_position UNIQUE (estimation_id, position)
);

CREATE INDEX idx_estimation_buckets_estimation ON estimation_buckets(estimation_id);

-- Bucketed leaf columns on the draft side.
ALTER TABLE draft_estimation_nodes
    ADD COLUMN bucket_id UUID NULL,
    ADD COLUMN is_sample BOOLEAN NULL;

ALTER TABLE draft_estimation_nodes
    ADD CONSTRAINT fk_draft_nodes_bucket FOREIGN KEY (bucket_id)
        REFERENCES estimation_buckets(id) ON DELETE RESTRICT;

CREATE INDEX idx_draft_nodes_bucket ON draft_estimation_nodes(bucket_id);

-- Bucketed leaf columns on the submitted side.
ALTER TABLE submitted_estimation_nodes
    ADD COLUMN bucket_id UUID NULL,
    ADD COLUMN is_sample BOOLEAN NULL;

ALTER TABLE submitted_estimation_nodes
    ADD CONSTRAINT fk_submitted_nodes_bucket FOREIGN KEY (bucket_id)
        REFERENCES estimation_buckets(id) ON DELETE RESTRICT;

CREATE INDEX idx_submitted_nodes_bucket ON submitted_estimation_nodes(bucket_id);
