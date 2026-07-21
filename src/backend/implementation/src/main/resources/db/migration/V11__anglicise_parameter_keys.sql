-- Anglicise the German semantic parameter keys (phase-16, task-121).
-- The domain now looks up parameters by English keys; rename the persisted
-- rows in both parameter tables so existing drafts + submitted versions keep
-- resolving their daily rate, std-deviation factor, and sales surcharge.
UPDATE draft_estimation_parameters SET name = 'dailyRate' WHERE name = 'Tagessatz';
UPDATE draft_estimation_parameters SET name = 'stdDevFactor' WHERE name = 'Standardabweichungsfaktor';
UPDATE draft_estimation_parameters SET name = 'salesSurcharge' WHERE name = 'Vertriebszuschlag';

UPDATE submitted_estimation_parameters SET name = 'dailyRate' WHERE name = 'Tagessatz';
UPDATE submitted_estimation_parameters SET name = 'stdDevFactor' WHERE name = 'Standardabweichungsfaktor';
UPDATE submitted_estimation_parameters SET name = 'salesSurcharge' WHERE name = 'Vertriebszuschlag';
