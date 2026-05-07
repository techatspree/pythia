ALTER TABLE estimations DROP CONSTRAINT IF EXISTS fk_estimations_current_version;
DROP INDEX IF EXISTS idx_estimations_current_version;
ALTER TABLE estimations DROP COLUMN IF EXISTS current_version_id;
