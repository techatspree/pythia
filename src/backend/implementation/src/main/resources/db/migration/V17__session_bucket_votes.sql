-- task-106: bucket + sampled sessions vote on a BUCKET ASSIGNMENT, which the
-- V13 session_votes shape (the THREE_POINT_PERT triple) cannot express.
--
-- Mirrors the node tables' bucket columns from V10 (`bucket_id` + `is_sample`)
-- so the two sides describe a bucketed value the same way. Both are NULLable:
-- a PERT session's votes leave them empty, and the existing min/expected/
-- max_effort columns stay NOT NULL — a bucket vote that also carries a sample
-- reuses that triple, exactly as a BUCKETED node does.
--
-- ON DELETE RESTRICT, not CASCADE: deleting a bucket that a cast vote still
-- references should fail loudly rather than silently rewrite session history.
ALTER TABLE session_votes
    ADD COLUMN bucket_id UUID NULL,
    ADD COLUMN is_sample BOOLEAN NULL;

ALTER TABLE session_votes
    ADD CONSTRAINT fk_session_votes_bucket FOREIGN KEY (bucket_id)
        REFERENCES estimation_buckets(id) ON DELETE RESTRICT;

CREATE INDEX idx_session_votes_bucket ON session_votes(bucket_id);
