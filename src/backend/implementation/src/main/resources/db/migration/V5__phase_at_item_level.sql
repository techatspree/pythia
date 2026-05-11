ALTER TABLE submitted_estimation_items ADD COLUMN phase_abbreviation VARCHAR(255);
ALTER TABLE submitted_estimation_item_groups DROP COLUMN phase_abbreviation;
ALTER TABLE draft_estimation_item_groups DROP CONSTRAINT IF EXISTS fk_draft_groups_phase;
ALTER TABLE draft_estimation_item_groups DROP COLUMN IF EXISTS phase_id;
