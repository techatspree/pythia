-- task-213: accompanying work is entered in HOURS per week; it becomes
-- person-days by dividing by the hours of a working day. That number is a
-- per-installation setting, default 8.
--
-- Both columns are also declared on their entities (SystemSettings,
-- SubmittedEstimationVersion), constraints included: %test and %dev build the
-- schema from Hibernate with Flyway off (the V15 lesson, src/backend/CLAUDE.md).
ALTER TABLE system_settings
    ADD COLUMN hours_per_day DOUBLE PRECISION NOT NULL DEFAULT 8,
    ADD CONSTRAINT ck_system_settings_hours_per_day CHECK (hours_per_day > 0 AND hours_per_day <= 24);

-- The hours per day a submitted version was calculated with, so a later change
-- of the setting can be explained rather than looking like a re-estimate.
-- NULLABLE on purpose, and existing rows stay NULL: they were calculated BEFORE
-- this conversion existed, with hours counted as person-days, so stamping 8 on
-- them would be false. NULL means "calculated before the conversion".
ALTER TABLE submitted_estimation_versions
    ADD COLUMN hours_per_day DOUBLE PRECISION,
    ADD CONSTRAINT ck_submitted_versions_hours_per_day
        CHECK (hours_per_day IS NULL OR (hours_per_day > 0 AND hours_per_day <= 24));
