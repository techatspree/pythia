CREATE TABLE submitted_estimation_nodes (
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
    phase_abbreviation VARCHAR(255) NULL,
    mean DOUBLE PRECISION NOT NULL,
    variance DOUBLE PRECISION NOT NULL,
    risk_surcharge DOUBLE PRECISION NOT NULL,
    driver_surcharge DOUBLE PRECISION NOT NULL,
    offer_pt DOUBLE PRECISION NOT NULL,
    cost DOUBLE PRECISION NOT NULL,
    offer_price DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_submitted_nodes_version FOREIGN KEY (version_id)
        REFERENCES submitted_estimation_versions(id),
    CONSTRAINT fk_submitted_nodes_parent FOREIGN KEY (parent_id)
        REFERENCES submitted_estimation_nodes(id)
);

CREATE INDEX idx_submitted_nodes_version ON submitted_estimation_nodes(version_id);
CREATE INDEX idx_submitted_nodes_parent  ON submitted_estimation_nodes(parent_id);

-- Migrate every submitted_estimation_item_groups row into a GROUP node.
-- Both old submitted tables have no created_at column — ORDER BY id only.
-- GROUP rows' accumulated columns come from SUM(...) over their items.
INSERT INTO submitted_estimation_nodes (
    id, logical_id, version_id, parent_id, node_type, position,
    title,
    mean, variance, risk_surcharge, driver_surcharge,
    offer_pt, cost, offer_price
)
SELECT
    g.id,
    g.logical_id,
    g.version_id,
    NULL,
    'GROUP',
    ROW_NUMBER() OVER (PARTITION BY g.version_id ORDER BY g.id) - 1,
    g.title,
    COALESCE((SELECT SUM(i.mean)             FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.variance)         FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.risk_surcharge)   FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.driver_surcharge) FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.offer_pt)         FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.cost)             FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0),
    COALESCE((SELECT SUM(i.offer_price)      FROM submitted_estimation_items i WHERE i.group_id = g.id), 0.0)
FROM submitted_estimation_item_groups g;

-- Migrate every submitted_estimation_items row into a leaf node under its group.
INSERT INTO submitted_estimation_nodes (
    id, logical_id, version_id, parent_id, node_type, position,
    description, code, min_effort, expected_effort, max_effort,
    assumptions, unit, phase_abbreviation,
    mean, variance, risk_surcharge, driver_surcharge,
    offer_pt, cost, offer_price
)
SELECT
    i.id,
    i.logical_id,
    g.version_id,
    i.group_id,
    i.item_type,
    ROW_NUMBER() OVER (PARTITION BY i.group_id ORDER BY i.id) - 1,
    i.description,
    i.code,
    i.min_effort,
    i.expected_effort,
    i.max_effort,
    i.assumptions,
    i.unit,
    i.phase_abbreviation,
    i.mean,
    i.variance,
    i.risk_surcharge,
    i.driver_surcharge,
    i.offer_pt,
    i.cost,
    i.offer_price
FROM submitted_estimation_items i
JOIN submitted_estimation_item_groups g ON i.group_id = g.id;

DROP TABLE submitted_estimation_items;
DROP TABLE submitted_estimation_item_groups;
