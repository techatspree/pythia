ALTER TABLE estimation_items ADD COLUMN logical_id UUID;
UPDATE estimation_items SET logical_id = id WHERE logical_id IS NULL;
ALTER TABLE estimation_items ALTER COLUMN logical_id SET NOT NULL;

ALTER TABLE estimation_item_groups ADD COLUMN logical_id UUID;
UPDATE estimation_item_groups SET logical_id = id WHERE logical_id IS NULL;
ALTER TABLE estimation_item_groups ALTER COLUMN logical_id SET NOT NULL;
