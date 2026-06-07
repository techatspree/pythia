CREATE TABLE draft_estimation_nodes (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    version_id UUID NOT NULL,
    parent_id UUID NULL,
    node_type VARCHAR(31) NOT NULL,
    position INT NOT NULL,
    title VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    min_effort DOUBLE PRECISION NULL,
    expected_effort DOUBLE PRECISION NULL,
    max_effort DOUBLE PRECISION NULL,
    assumptions TEXT NULL,
    unit VARCHAR(255) NULL,
    phase_id UUID NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_draft_nodes_version FOREIGN KEY (version_id)
        REFERENCES draft_estimation_versions(id) ON DELETE CASCADE,
    CONSTRAINT fk_draft_nodes_parent FOREIGN KEY (parent_id)
        REFERENCES draft_estimation_nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_draft_nodes_phase FOREIGN KEY (phase_id)
        REFERENCES draft_project_phases(id)
);

CREATE INDEX idx_draft_nodes_version ON draft_estimation_nodes(version_id);
CREATE INDEX idx_draft_nodes_parent  ON draft_estimation_nodes(parent_id);
CREATE INDEX idx_draft_nodes_phase   ON draft_estimation_nodes(phase_id);

-- Migrate every draft_estimation_item_groups row into a GROUP node
-- (ORDER BY id only: V3's groups table has no created_at column).
-- groups.phase_id is dropped — tree-model groups carry no phase, and any
-- per-item phase was already stored at the item level by V5.
INSERT INTO draft_estimation_nodes (
    id, logical_id, version_id, parent_id, node_type, position,
    title, created_at
)
SELECT
    g.id,
    g.logical_id,
    g.version_id,
    NULL,
    'GROUP',
    ROW_NUMBER() OVER (PARTITION BY g.version_id ORDER BY g.id) - 1,
    g.title,
    CURRENT_TIMESTAMP
FROM draft_estimation_item_groups g;

-- Migrate every draft_estimation_items row into a leaf node under its
-- old group's new GROUP row (ORDER BY created_at, id).
INSERT INTO draft_estimation_nodes (
    id, logical_id, version_id, parent_id, node_type, position,
    description, code, min_effort, expected_effort, max_effort,
    assumptions, unit, phase_id, created_at, updated_at
)
SELECT
    i.id,
    i.logical_id,
    g.version_id,
    i.group_id,
    i.item_type,
    ROW_NUMBER() OVER (PARTITION BY i.group_id ORDER BY i.created_at, i.id) - 1,
    i.description,
    i.code,
    i.min_effort,
    i.expected_effort,
    i.max_effort,
    i.assumptions,
    i.unit,
    i.phase_id,
    i.created_at,
    i.updated_at
FROM draft_estimation_items i
JOIN draft_estimation_item_groups g ON i.group_id = g.id;

DROP TABLE draft_estimation_items;
DROP TABLE draft_estimation_item_groups;
