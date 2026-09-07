-- task-177: where a phase's length comes from.
--
-- DEFAULT 'EXPLICIT' is not a formality: every version persisted before this
-- column existed carries a length somebody typed, and that must keep behaving
-- exactly as it did. AUTOMATIC is opt-in per phase.
--
-- The column is also declared on DraftProjectPhase / SubmittedProjectPhase.
-- That duplication is deliberate: %test and %dev build the schema from
-- Hibernate with Flyway off, so a migration-only column would not exist where
-- it is tested (the V15 lesson, recorded in src/backend/CLAUDE.md).
ALTER TABLE draft_project_phases
    ADD COLUMN duration_mode VARCHAR(255) NOT NULL DEFAULT 'EXPLICIT';

ALTER TABLE submitted_project_phases
    ADD COLUMN duration_mode VARCHAR(255) NOT NULL DEFAULT 'EXPLICIT';
