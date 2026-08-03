-- Whether the session moderator also participates as an estimator (task-129).
-- Default true preserves the prior behaviour where the moderator always votes.
ALTER TABLE estimation_sessions ADD COLUMN moderator_estimates boolean NOT NULL DEFAULT true;
