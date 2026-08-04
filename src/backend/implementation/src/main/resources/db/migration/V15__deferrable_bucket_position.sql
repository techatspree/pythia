-- Reordering buckets swaps their `position` values, and DraftUpdateApplier
-- applies those as row-by-row UPDATEs. PostgreSQL evaluates a plain UNIQUE
-- constraint after EVERY statement, so the transient state in the middle of a
-- swap — two rows briefly holding the same position — aborted the whole
-- transaction:
--
--   ERROR: duplicate key value violates unique constraint
--          "uq_estimation_buckets_position"
--   Detail: Key (estimation_id, "position")=(…, 0) already exists.
--
-- The user-visible effect was "the change could not be saved" on dragging a
-- bucket to the front, with the reorder silently lost. Whether it tripped
-- depended on Hibernate's flush order, so it failed intermittently rather than
-- always.
--
-- Making the constraint DEFERRABLE INITIALLY DEFERRED moves the check to
-- COMMIT: intermediate duplicates are allowed, the final state must still be
-- unique. The invariant is unchanged — only when it is enforced.
ALTER TABLE estimation_buckets
    DROP CONSTRAINT uq_estimation_buckets_position;

ALTER TABLE estimation_buckets
    ADD CONSTRAINT uq_estimation_buckets_position
        UNIQUE (estimation_id, position)
        DEFERRABLE INITIALLY DEFERRED;
