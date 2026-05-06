-- Draft tables: user-editable input fields only, no calculated columns
-- At most one draft per estimation (UNIQUE on estimation_id)

CREATE TABLE draft_estimation_versions (
    id UUID PRIMARY KEY,
    version_number INTEGER NOT NULL,
    notes TEXT,
    estimation_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_draft_estimation UNIQUE (estimation_id),
    CONSTRAINT fk_draft_versions_estimation FOREIGN KEY (estimation_id) REFERENCES estimations(id)
);

CREATE TABLE draft_estimation_parameters (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    param_value DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_draft_params_version FOREIGN KEY (version_id) REFERENCES draft_estimation_versions(id) ON DELETE CASCADE
);

CREATE TABLE draft_effort_drivers (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    factor DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_draft_drivers_version FOREIGN KEY (version_id) REFERENCES draft_estimation_versions(id) ON DELETE CASCADE
);

CREATE TABLE draft_project_phases (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    abbreviation VARCHAR(255) NOT NULL,
    duration_weeks DOUBLE PRECISION,
    version_id UUID NOT NULL,
    CONSTRAINT fk_draft_phases_version FOREIGN KEY (version_id) REFERENCES draft_estimation_versions(id) ON DELETE CASCADE
);

CREATE TABLE draft_estimation_item_groups (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    phase_id UUID,
    version_id UUID NOT NULL,
    CONSTRAINT fk_draft_groups_phase FOREIGN KEY (phase_id) REFERENCES draft_project_phases(id),
    CONSTRAINT fk_draft_groups_version FOREIGN KEY (version_id) REFERENCES draft_estimation_versions(id) ON DELETE CASCADE
);

CREATE TABLE draft_estimation_items (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    item_type VARCHAR(31) NOT NULL,
    description VARCHAR(255) NOT NULL,
    code VARCHAR(255),
    min_effort DOUBLE PRECISION,
    expected_effort DOUBLE PRECISION,
    max_effort DOUBLE PRECISION,
    assumptions TEXT,
    unit VARCHAR(255),
    phase_id UUID,
    group_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_draft_items_phase FOREIGN KEY (phase_id) REFERENCES draft_project_phases(id),
    CONSTRAINT fk_draft_items_group FOREIGN KEY (group_id) REFERENCES draft_estimation_item_groups(id) ON DELETE CASCADE
);

CREATE TABLE draft_additional_costs (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(255) NOT NULL,
    amount_per_week DOUBLE PRECISION,
    phase_id UUID,
    version_id UUID NOT NULL,
    CONSTRAINT fk_draft_costs_phase FOREIGN KEY (phase_id) REFERENCES draft_project_phases(id),
    CONSTRAINT fk_draft_costs_version FOREIGN KEY (version_id) REFERENCES draft_estimation_versions(id) ON DELETE CASCADE
);

-- Submitted tables: immutable snapshots with all calculated results

CREATE TABLE submitted_estimation_versions (
    id UUID PRIMARY KEY,
    version_number INTEGER NOT NULL,
    total_effort DOUBLE PRECISION NOT NULL,
    notes TEXT,
    estimation_id UUID NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_submitted_versions_estimation FOREIGN KEY (estimation_id) REFERENCES estimations(id),
    CONSTRAINT uq_submitted_estimation_version UNIQUE (estimation_id, version_number)
);

CREATE TABLE submitted_estimation_parameters (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    param_value DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_submitted_params_version FOREIGN KEY (version_id) REFERENCES submitted_estimation_versions(id)
);

CREATE TABLE submitted_effort_drivers (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    factor DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_submitted_drivers_version FOREIGN KEY (version_id) REFERENCES submitted_estimation_versions(id)
);

CREATE TABLE submitted_project_phases (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    abbreviation VARCHAR(255) NOT NULL,
    duration_weeks DOUBLE PRECISION,
    version_id UUID NOT NULL,
    CONSTRAINT fk_submitted_phases_version FOREIGN KEY (version_id) REFERENCES submitted_estimation_versions(id)
);

CREATE TABLE submitted_estimation_item_groups (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    phase_abbreviation VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_submitted_groups_version FOREIGN KEY (version_id) REFERENCES submitted_estimation_versions(id)
);

CREATE TABLE submitted_estimation_items (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    item_type VARCHAR(31) NOT NULL,
    description VARCHAR(255) NOT NULL,
    code VARCHAR(255),
    min_effort DOUBLE PRECISION NOT NULL,
    expected_effort DOUBLE PRECISION NOT NULL,
    max_effort DOUBLE PRECISION NOT NULL,
    mean DOUBLE PRECISION NOT NULL,
    variance DOUBLE PRECISION NOT NULL,
    risk_surcharge DOUBLE PRECISION NOT NULL,
    driver_surcharge DOUBLE PRECISION NOT NULL,
    offer_pt DOUBLE PRECISION NOT NULL,
    cost DOUBLE PRECISION NOT NULL,
    offer_price DOUBLE PRECISION NOT NULL,
    assumptions TEXT,
    unit VARCHAR(255),
    group_id UUID NOT NULL,
    CONSTRAINT fk_submitted_items_group FOREIGN KEY (group_id) REFERENCES submitted_estimation_item_groups(id)
);

CREATE TABLE submitted_additional_costs (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(255) NOT NULL,
    amount_per_week DOUBLE PRECISION,
    phase_abbreviation VARCHAR(255),
    version_id UUID NOT NULL,
    CONSTRAINT fk_submitted_costs_version FOREIGN KEY (version_id) REFERENCES submitted_estimation_versions(id)
);

-- Migrate existing data

-- Move SUBMITTED versions to submitted tables
INSERT INTO submitted_estimation_versions (id, version_number, total_effort, notes, estimation_id, submitted_at, created_at)
SELECT id, version_number, COALESCE(total_effort, 0.0), notes, estimation_id, COALESCE(updated_at, created_at), created_at
FROM estimation_versions WHERE status = 'SUBMITTED';

INSERT INTO submitted_estimation_parameters (id, name, param_value, comment, version_id)
SELECT ep.id, ep.name, ep.value, ep.comment, ep.version_id
FROM estimation_parameters ep
JOIN estimation_versions ev ON ep.version_id = ev.id
WHERE ev.status = 'SUBMITTED';

INSERT INTO submitted_effort_drivers (id, description, factor, comment, version_id)
SELECT ed.id, ed.description, ed.factor, ed.comment, ed.version_id
FROM effort_drivers ed
JOIN estimation_versions ev ON ed.version_id = ev.id
WHERE ev.status = 'SUBMITTED';

INSERT INTO submitted_project_phases (id, name, abbreviation, duration_weeks, version_id)
SELECT pp.id, pp.name, pp.abbreviation, pp.duration_weeks, pp.version_id
FROM project_phases pp
JOIN estimation_versions ev ON pp.version_id = ev.id
WHERE ev.status = 'SUBMITTED';

INSERT INTO submitted_estimation_item_groups (id, logical_id, title, phase_abbreviation, version_id)
SELECT eig.id, eig.logical_id, eig.title, pp.abbreviation, eig.version_id
FROM estimation_item_groups eig
JOIN estimation_versions ev ON eig.version_id = ev.id
LEFT JOIN project_phases pp ON eig.phase_id = pp.id
WHERE ev.status = 'SUBMITTED';

INSERT INTO submitted_estimation_items (id, logical_id, item_type, description, code, min_effort, expected_effort, max_effort, mean, variance, risk_surcharge, driver_surcharge, offer_pt, cost, offer_price, assumptions, unit, group_id)
SELECT ei.id, ei.logical_id, ei.item_type, ei.description, ei.code,
       COALESCE(ei.min_effort, 0.0), COALESCE(ei.expected_effort, 0.0), COALESCE(ei.max_effort, 0.0),
       COALESCE(ei.mean, 0.0), COALESCE(ei.variance, 0.0),
       COALESCE(ei.risk_surcharge, 0.0), COALESCE(ei.driver_surcharge, 0.0),
       COALESCE(ei.offer_pt, 0.0), COALESCE(ei.cost, 0.0), COALESCE(ei.offer_price, 0.0),
       ei.assumptions, ei.unit, ei.group_id
FROM estimation_items ei
JOIN estimation_item_groups eig ON ei.group_id = eig.id
JOIN estimation_versions ev ON eig.version_id = ev.id
WHERE ev.status = 'SUBMITTED';

INSERT INTO submitted_additional_costs (id, description, amount, type, amount_per_week, phase_abbreviation, version_id)
SELECT ac.id, ac.description, ac.amount, ac.type, ac.amount_per_week, pp.abbreviation, ac.version_id
FROM additional_costs ac
JOIN estimation_versions ev ON ac.version_id = ev.id
LEFT JOIN project_phases pp ON ac.phase_id = pp.id
WHERE ev.status = 'SUBMITTED';

-- Move DRAFT versions to draft tables
INSERT INTO draft_estimation_versions (id, version_number, notes, estimation_id, created_at, updated_at)
SELECT id, version_number, notes, estimation_id, created_at, updated_at
FROM estimation_versions WHERE status = 'DRAFT';

INSERT INTO draft_estimation_parameters (id, name, param_value, comment, version_id)
SELECT ep.id, ep.name, ep.value, ep.comment, ep.version_id
FROM estimation_parameters ep
JOIN estimation_versions ev ON ep.version_id = ev.id
WHERE ev.status = 'DRAFT';

INSERT INTO draft_effort_drivers (id, description, factor, comment, version_id)
SELECT ed.id, ed.description, ed.factor, ed.comment, ed.version_id
FROM effort_drivers ed
JOIN estimation_versions ev ON ed.version_id = ev.id
WHERE ev.status = 'DRAFT';

INSERT INTO draft_project_phases (id, name, abbreviation, duration_weeks, version_id)
SELECT pp.id, pp.name, pp.abbreviation, pp.duration_weeks, pp.version_id
FROM project_phases pp
JOIN estimation_versions ev ON pp.version_id = ev.id
WHERE ev.status = 'DRAFT';

INSERT INTO draft_estimation_item_groups (id, logical_id, title, phase_id, version_id)
SELECT eig.id, eig.logical_id, eig.title, eig.phase_id, eig.version_id
FROM estimation_item_groups eig
JOIN estimation_versions ev ON eig.version_id = ev.id
WHERE ev.status = 'DRAFT';

INSERT INTO draft_estimation_items (id, logical_id, item_type, description, code, min_effort, expected_effort, max_effort, assumptions, unit, phase_id, group_id, created_at, updated_at)
SELECT ei.id, ei.logical_id, ei.item_type, ei.description, ei.code,
       ei.min_effort, ei.expected_effort, ei.max_effort,
       ei.assumptions, ei.unit, ei.phase_id, ei.group_id, ei.created_at, ei.updated_at
FROM estimation_items ei
JOIN estimation_item_groups eig ON ei.group_id = eig.id
JOIN estimation_versions ev ON eig.version_id = ev.id
WHERE ev.status = 'DRAFT';

INSERT INTO draft_additional_costs (id, description, amount, type, amount_per_week, phase_id, version_id)
SELECT ac.id, ac.description, ac.amount, ac.type, ac.amount_per_week, ac.phase_id, ac.version_id
FROM additional_costs ac
JOIN estimation_versions ev ON ac.version_id = ev.id
WHERE ev.status = 'DRAFT';

-- Update estimations FK to point to submitted_estimation_versions
ALTER TABLE estimations DROP CONSTRAINT fk_estimations_current_version;
ALTER TABLE estimations ADD CONSTRAINT fk_estimations_current_version
    FOREIGN KEY (current_version_id) REFERENCES submitted_estimation_versions(id);

-- Drop old tables (order matters due to FK dependencies)
DROP TABLE IF EXISTS additional_costs;
DROP TABLE IF EXISTS estimation_items;
DROP TABLE IF EXISTS estimation_item_groups;
DROP TABLE IF EXISTS project_phases;
DROP TABLE IF EXISTS effort_drivers;
DROP TABLE IF EXISTS estimation_parameters;
DROP TABLE IF EXISTS estimation_versions;

-- Indexes
CREATE INDEX idx_draft_versions_estimation ON draft_estimation_versions(estimation_id);
CREATE INDEX idx_draft_params_version ON draft_estimation_parameters(version_id);
CREATE INDEX idx_draft_drivers_version ON draft_effort_drivers(version_id);
CREATE INDEX idx_draft_phases_version ON draft_project_phases(version_id);
CREATE INDEX idx_draft_groups_version ON draft_estimation_item_groups(version_id);
CREATE INDEX idx_draft_items_group ON draft_estimation_items(group_id);
CREATE INDEX idx_draft_costs_version ON draft_additional_costs(version_id);

CREATE INDEX idx_submitted_versions_estimation ON submitted_estimation_versions(estimation_id);
CREATE INDEX idx_submitted_params_version ON submitted_estimation_parameters(version_id);
CREATE INDEX idx_submitted_drivers_version ON submitted_effort_drivers(version_id);
CREATE INDEX idx_submitted_phases_version ON submitted_project_phases(version_id);
CREATE INDEX idx_submitted_groups_version ON submitted_estimation_item_groups(version_id);
CREATE INDEX idx_submitted_items_group ON submitted_estimation_items(group_id);
CREATE INDEX idx_submitted_costs_version ON submitted_additional_costs(version_id);
