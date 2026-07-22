-- Per-user UI language preference (phase-16, task-123). Default 'de' so existing
-- users keep today's German UI; first-sighting seeding and the
-- PUT /api/auth/me/language endpoint may change it. Codes mirror the
-- SupportedLanguage enum (io.github.theestimator.i18n) shared with the frontend.
ALTER TABLE users ADD COLUMN language char(2) NOT NULL DEFAULT 'de';
