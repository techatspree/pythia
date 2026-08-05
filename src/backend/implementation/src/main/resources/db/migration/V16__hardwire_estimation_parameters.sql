-- The three values the calculation depends on (daily rate, std-deviation
-- factor, sales surcharge) were stored as GENERIC name/value rows and looked up
-- by the English strings "dailyRate"/"stdDevFactor"/"salesSurcharge". The GUI
-- let users edit those names, and a rename made the lookup miss and SILENTLY
-- fall back to the default — observed in production, where a row had been
-- renamed to "Tagessatz" and its estimation was quietly computing at 800 €/day.
--
-- They become first-class columns, so they cannot be renamed away (task-138).
ALTER TABLE draft_estimation_versions
    ADD COLUMN daily_rate      DOUBLE PRECISION NOT NULL DEFAULT 800,
    ADD COLUMN std_dev_factor  DOUBLE PRECISION NOT NULL DEFAULT 2,
    ADD COLUMN sales_surcharge DOUBLE PRECISION NOT NULL DEFAULT 0.1;

ALTER TABLE submitted_estimation_versions
    ADD COLUMN daily_rate      DOUBLE PRECISION NOT NULL DEFAULT 800,
    ADD COLUMN std_dev_factor  DOUBLE PRECISION NOT NULL DEFAULT 2,
    ADD COLUMN sales_surcharge DOUBLE PRECISION NOT NULL DEFAULT 0.1;

-- Backfill from the parameter rows. Canonical English names ONLY: the one row
-- that had been renamed has since been renamed back, so there is nothing to
-- translate and no alias handling here on purpose. A row matching no canonical
-- name simply leaves the column at its default.
-- (The value column is `param_value`; the owner is `version_id`.)
UPDATE draft_estimation_versions v SET daily_rate = p.param_value
    FROM draft_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'dailyRate';
UPDATE draft_estimation_versions v SET std_dev_factor = p.param_value
    FROM draft_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'stdDevFactor';
UPDATE draft_estimation_versions v SET sales_surcharge = p.param_value
    FROM draft_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'salesSurcharge';

UPDATE submitted_estimation_versions v SET daily_rate = p.param_value
    FROM submitted_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'dailyRate';
UPDATE submitted_estimation_versions v SET std_dev_factor = p.param_value
    FROM submitted_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'stdDevFactor';
UPDATE submitted_estimation_versions v SET sales_surcharge = p.param_value
    FROM submitted_estimation_parameters p
    WHERE p.version_id = v.id AND p.name = 'salesSurcharge';

DROP TABLE draft_estimation_parameters;
DROP TABLE submitted_estimation_parameters;
