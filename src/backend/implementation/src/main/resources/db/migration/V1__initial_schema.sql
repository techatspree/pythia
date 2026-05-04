CREATE TABLE users (
    id UUID PRIMARY KEY,
    entra_subject_id VARCHAR(255) UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    client VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    owner_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE estimations (
    id UUID PRIMARY KEY,
    offer VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    project_id UUID NOT NULL,
    current_version_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_estimations_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE estimation_versions (
    id UUID PRIMARY KEY,
    version_number INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    total_effort DOUBLE PRECISION,
    notes VARCHAR(255),
    estimation_id UUID NOT NULL,
    created_by_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_versions_estimation FOREIGN KEY (estimation_id) REFERENCES estimations(id),
    CONSTRAINT fk_versions_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT uq_estimation_version UNIQUE (estimation_id, version_number)
);

ALTER TABLE estimations
    ADD CONSTRAINT fk_estimations_current_version FOREIGN KEY (current_version_id) REFERENCES estimation_versions(id);

CREATE TABLE estimation_parameters (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_parameters_version FOREIGN KEY (version_id) REFERENCES estimation_versions(id)
);

CREATE TABLE effort_drivers (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    factor DOUBLE PRECISION NOT NULL,
    comment VARCHAR(255),
    version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_drivers_version FOREIGN KEY (version_id) REFERENCES estimation_versions(id)
);

CREATE TABLE project_phases (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    abbreviation VARCHAR(255) NOT NULL,
    duration_weeks DOUBLE PRECISION,
    version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_phases_version FOREIGN KEY (version_id) REFERENCES estimation_versions(id)
);

CREATE TABLE estimation_item_groups (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    phase_id UUID,
    version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_groups_phase FOREIGN KEY (phase_id) REFERENCES project_phases(id),
    CONSTRAINT fk_groups_version FOREIGN KEY (version_id) REFERENCES estimation_versions(id)
);

CREATE TABLE estimation_items (
    id UUID PRIMARY KEY,
    item_type VARCHAR(31) NOT NULL,
    description VARCHAR(255) NOT NULL,
    code VARCHAR(255),
    min_effort DOUBLE PRECISION,
    expected_effort DOUBLE PRECISION,
    max_effort DOUBLE PRECISION,
    assumptions VARCHAR(255),
    mean DOUBLE PRECISION,
    variance DOUBLE PRECISION,
    risk_surcharge DOUBLE PRECISION,
    driver_surcharge DOUBLE PRECISION,
    offer_pt DOUBLE PRECISION,
    cost DOUBLE PRECISION,
    offer_price DOUBLE PRECISION,
    unit VARCHAR(255),
    phase_id UUID,
    group_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_items_phase FOREIGN KEY (phase_id) REFERENCES project_phases(id),
    CONSTRAINT fk_items_group FOREIGN KEY (group_id) REFERENCES estimation_item_groups(id)
);

CREATE TABLE additional_costs (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(255) NOT NULL,
    amount_per_week DOUBLE PRECISION,
    phase_id UUID,
    version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_costs_phase FOREIGN KEY (phase_id) REFERENCES project_phases(id),
    CONSTRAINT fk_costs_version FOREIGN KEY (version_id) REFERENCES estimation_versions(id)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    entity_type VARCHAR(255),
    entity_id UUID,
    action VARCHAR(255),
    payload TEXT,
    created_at TIMESTAMP NOT NULL
);

-- Indexes on foreign key columns
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_estimations_project ON estimations(project_id);
CREATE INDEX idx_estimations_current_version ON estimations(current_version_id);
CREATE INDEX idx_versions_estimation ON estimation_versions(estimation_id);
CREATE INDEX idx_versions_created_by ON estimation_versions(created_by_id);
CREATE INDEX idx_parameters_version ON estimation_parameters(version_id);
CREATE INDEX idx_drivers_version ON effort_drivers(version_id);
CREATE INDEX idx_phases_version ON project_phases(version_id);
CREATE INDEX idx_groups_phase ON estimation_item_groups(phase_id);
CREATE INDEX idx_groups_version ON estimation_item_groups(version_id);
CREATE INDEX idx_items_phase ON estimation_items(phase_id);
CREATE INDEX idx_items_group ON estimation_items(group_id);
CREATE INDEX idx_costs_phase ON additional_costs(phase_id);
CREATE INDEX idx_costs_version ON additional_costs(version_id);
