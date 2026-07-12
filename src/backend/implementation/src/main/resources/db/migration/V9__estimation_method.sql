-- Bind every estimation to exactly one estimation method (task-099). The
-- method is chosen at creation and immutable thereafter. VARCHAR(255) matches
-- the other enum columns and the entity's @Enumerated(STRING) mapping; the
-- DEFAULT backfills existing rows and stays as belt-and-braces.
ALTER TABLE estimations
    ADD COLUMN method VARCHAR(255) NOT NULL
    DEFAULT 'THREE_POINT_PERT';
