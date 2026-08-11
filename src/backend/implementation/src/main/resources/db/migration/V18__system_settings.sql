-- Per-installation system settings (task-146).
--
-- system_settings is a SINGLETON: the CHECK (id = 1) makes a second row
-- unrepresentable, and the INSERT below seeds the one row so every read finds
-- it and no code path needs a lazy create. The same CHECK is declared on the
-- SystemSettings entity, because %test/%dev build the schema from Hibernate
-- with Flyway off (the V15 migration/entity parity lesson).
CREATE TABLE system_settings (
    id                    SMALLINT     NOT NULL DEFAULT 1,
    display_name          VARCHAR(200),
    custom_css            TEXT,
    custom_css_filename   VARCHAR(255),
    custom_css_updated_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_system_settings PRIMARY KEY (id),
    CONSTRAINT ck_system_settings_singleton CHECK (id = 1)
);

INSERT INTO system_settings (id) VALUES (1);

-- The organisation-wide effort-driver template, copied into a draft that has no
-- submitted version to clone from.
CREATE TABLE system_effort_drivers (
    id          UUID             NOT NULL PRIMARY KEY,
    position    INTEGER          NOT NULL,
    description VARCHAR(500)     NOT NULL,
    factor      DOUBLE PRECISION NOT NULL DEFAULT 0,
    comment     VARCHAR(2000)    NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT uq_system_effort_drivers_position UNIQUE (position)
);
