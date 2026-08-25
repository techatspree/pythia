-- Schedule inputs for phase-19 (task-156): the two values task-155's
-- EstimationVersion.schedule(dependencies, teamFte) needs but the system could
-- not store — the team size in FTE and the finish-to-start edges between root
-- nodes.
--
-- Both sides get them. A submitted version is an immutable snapshot, and a
-- snapshot that lost its schedule inputs would render a different Gantt than
-- the draft it came from.
--
-- DOUBLE PRECISION, like V16 typed daily_rate/std_dev_factor/sales_surcharge
-- for the same JPA Double. The DEFAULT 1 matters: every existing row gets a
-- usable schedule immediately, and 0 is the one team size task-155 rejects.
ALTER TABLE draft_estimation_versions
    ADD COLUMN team_fte DOUBLE PRECISION NOT NULL DEFAULT 1;

ALTER TABLE submitted_estimation_versions
    ADD COLUMN team_fte DOUBLE PRECISION NOT NULL DEFAULT 1;

-- Dependencies are ROWS, not a JSON column: this repo models collections as
-- tables (the bucket and additional-cost tables), and the UNIQUE constraint is
-- what stops the editor from persisting a duplicate edge.
--
-- The same constraint is declared on the ENTITY as well — under %test/%dev the
-- schema is built by Hibernate with Flyway off, so a migration-only constraint
-- would not exist where it is tested (the V15 lesson).
CREATE TABLE draft_schedule_dependencies (
    id              UUID PRIMARY KEY,
    version_id      UUID NOT NULL REFERENCES draft_estimation_versions (id) ON DELETE CASCADE,
    from_logical_id TEXT NOT NULL,
    to_logical_id   TEXT NOT NULL,
    CONSTRAINT uq_draft_schedule_dep UNIQUE (version_id, from_logical_id, to_logical_id)
);

CREATE TABLE submitted_schedule_dependencies (
    id              UUID PRIMARY KEY,
    version_id      UUID NOT NULL REFERENCES submitted_estimation_versions (id) ON DELETE CASCADE,
    from_logical_id TEXT NOT NULL,
    to_logical_id   TEXT NOT NULL,
    CONSTRAINT uq_submitted_schedule_dep UNIQUE (version_id, from_logical_id, to_logical_id)
);
