-- Dev-local test data (only loaded with hibernate drop-and-create)
-- 2 Projects with estimations, submitted versions, and one draft

INSERT INTO projects (id, name, description, client, status, created_at) VALUES
('a1000000-0000-0000-0000-000000000001', 'Webshop Redesign', 'Complete redesign of the e-commerce platform', 'RetailCorp', 'ACTIVE', '2026-01-15 10:00:00'),
('a1000000-0000-0000-0000-000000000002', 'Mobile App MVP', 'Native iOS/Android app for customer self-service', 'FinanceAG', 'ACTIVE', '2026-02-01 09:00:00');

INSERT INTO estimations (id, offer, description, project_id, created_at) VALUES
('b1000000-0000-0000-0000-000000000001', 'WS-2026-001', 'Initial estimation for webshop redesign', 'a1000000-0000-0000-0000-000000000001', '2026-01-20 14:00:00'),
('b1000000-0000-0000-0000-000000000002', 'MA-2026-001', 'MVP scope estimation', 'a1000000-0000-0000-0000-000000000002', '2026-02-05 11:00:00');

-- Submitted versions (immutable snapshots with calculated results)

INSERT INTO submitted_estimation_versions (id, version_number, total_effort, notes, estimation_id, submitted_at, created_at) VALUES
('c1000000-0000-0000-0000-000000000001', 1, 42.5, 'Initial scope', 'b1000000-0000-0000-0000-000000000001', '2026-01-25 10:00:00', '2026-01-20 14:00:00'),
('c1000000-0000-0000-0000-000000000003', 1, 65.0, 'Full MVP scope', 'b1000000-0000-0000-0000-000000000002', '2026-02-10 10:00:00', '2026-02-05 11:00:00');

UPDATE estimations SET current_version_id = 'c1000000-0000-0000-0000-000000000001' WHERE id = 'b1000000-0000-0000-0000-000000000001';
UPDATE estimations SET current_version_id = 'c1000000-0000-0000-0000-000000000003' WHERE id = 'b1000000-0000-0000-0000-000000000002';

INSERT INTO submitted_estimation_parameters (id, name, param_value, comment, version_id) VALUES
('d1000000-0000-0000-0000-000000000001', 'Standardabweichungsfaktor', 2.0, NULL, 'c1000000-0000-0000-0000-000000000001'),
('d1000000-0000-0000-0000-000000000002', 'Tagessatz', 900.0, NULL, 'c1000000-0000-0000-0000-000000000001'),
('d1000000-0000-0000-0000-000000000003', 'Vertriebszuschlag', 0.12, NULL, 'c1000000-0000-0000-0000-000000000001');

INSERT INTO submitted_project_phases (id, name, abbreviation, duration_weeks, version_id) VALUES
('e1000000-0000-0000-0000-000000000001', 'Konzeption', 'KO', 4.0, 'c1000000-0000-0000-0000-000000000001'),
('e1000000-0000-0000-0000-000000000002', 'Umsetzung', 'UM', 12.0, 'c1000000-0000-0000-0000-000000000001');

INSERT INTO submitted_estimation_item_groups (id, logical_id, title, phase_abbreviation, version_id) VALUES
('f1000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'U01: Frontend Redesign', 'UM', 'c1000000-0000-0000-0000-000000000001'),
('f1000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000002', 'U02: Backend API', 'UM', 'c1000000-0000-0000-0000-000000000001');

INSERT INTO submitted_estimation_items (id, logical_id, item_type, description, min_effort, expected_effort, max_effort, mean, variance, risk_surcharge, driver_surcharge, offer_pt, cost, offer_price, group_id) VALUES
('aa000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'FIXED', 'Product listing page', 3.0, 5.0, 8.0, 5.17, 0.69, 0.5, 0.0, 5.67, 5100.0, 5712.0, 'f1000000-0000-0000-0000-000000000001'),
('aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000002', 'FIXED', 'Shopping cart', 5.0, 8.0, 12.0, 8.17, 1.36, 0.8, 0.0, 8.97, 8070.0, 9038.4, 'f1000000-0000-0000-0000-000000000001'),
('aa000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000003', 'FIXED', 'REST API endpoints', 4.0, 6.0, 10.0, 6.33, 1.0, 0.6, 0.0, 6.93, 6240.0, 6988.8, 'f1000000-0000-0000-0000-000000000002'),
('aa000000-0000-0000-0000-000000000004', 'aa000000-0000-0000-0000-000000000004', 'FIXED', 'Database migration', 2.0, 3.0, 5.0, 3.17, 0.25, 0.3, 0.0, 3.47, 3120.0, 3494.4, 'f1000000-0000-0000-0000-000000000002');

-- Draft version for Webshop estimation (version 2, in progress)

INSERT INTO draft_estimation_versions (id, version_number, notes, estimation_id, created_at) VALUES
('c1000000-0000-0000-0000-000000000002', 2, 'Reduced scope after feedback', 'b1000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00');

INSERT INTO draft_estimation_parameters (id, name, param_value, comment, version_id) VALUES
('d1000000-0000-0000-0000-000000000004', 'Standardabweichungsfaktor', 2.0, NULL, 'c1000000-0000-0000-0000-000000000002'),
('d1000000-0000-0000-0000-000000000005', 'Tagessatz', 900.0, NULL, 'c1000000-0000-0000-0000-000000000002'),
('d1000000-0000-0000-0000-000000000006', 'Vertriebszuschlag', 0.12, NULL, 'c1000000-0000-0000-0000-000000000002');

INSERT INTO draft_project_phases (id, name, abbreviation, duration_weeks, version_id) VALUES
('e1000000-0000-0000-0000-000000000003', 'Konzeption', 'KO', 4.0, 'c1000000-0000-0000-0000-000000000002'),
('e1000000-0000-0000-0000-000000000004', 'Umsetzung', 'UM', 10.0, 'c1000000-0000-0000-0000-000000000002');

INSERT INTO draft_estimation_item_groups (id, logical_id, title, phase_id, version_id) VALUES
('f1000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001', 'U01: Frontend Redesign', 'e1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000002'),
('f1000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000002', 'U02: Backend API', 'e1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000002');

INSERT INTO draft_estimation_items (id, logical_id, item_type, description, min_effort, expected_effort, max_effort, group_id, created_at) VALUES
('aa000000-0000-0000-0000-000000000005', 'aa000000-0000-0000-0000-000000000001', 'FIXED', 'Product listing page', 2.0, 4.0, 6.0, 'f1000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('aa000000-0000-0000-0000-000000000006', 'aa000000-0000-0000-0000-000000000002', 'FIXED', 'Shopping cart', 4.0, 7.0, 10.0, 'f1000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('aa000000-0000-0000-0000-000000000007', 'aa000000-0000-0000-0000-000000000003', 'FIXED', 'REST API endpoints', 3.0, 5.0, 8.0, 'f1000000-0000-0000-0000-000000000004', '2026-02-10 16:00:00'),
('aa000000-0000-0000-0000-000000000008', 'aa000000-0000-0000-0000-000000000004', 'FIXED', 'Database migration', 1.0, 2.0, 4.0, 'f1000000-0000-0000-0000-000000000004', '2026-02-10 16:00:00');
