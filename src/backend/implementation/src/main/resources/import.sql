-- Dev-local test data (loaded with hibernate drop-and-create, dev-local and dev profiles)
-- Webshop Redesign: v1 submitted + v2 draft with phases, items, drivers, additional costs
-- Mobile App MVP:   v1 submitted with phases and items

-- ── Projects ──────────────────────────────────────────────────────────────────

INSERT INTO projects (id, name, description, client, status, created_at) VALUES
('a1000000-0000-0000-0000-000000000001', 'Webshop Redesign',  'Komplette Neuentwicklung der E-Commerce-Plattform', 'RetailCorp GmbH', 'ACTIVE', '2026-01-15 10:00:00'),
('a1000000-0000-0000-0000-000000000002', 'Mobile App MVP',    'Native iOS/Android-App für Kunden-Self-Service',   'FinanceAG',       'ACTIVE', '2026-02-01 09:00:00');

INSERT INTO estimations (id, offer, description, project_id, created_at) VALUES
('b1000000-0000-0000-0000-000000000001', 'WS-2026-001', 'Erstschätzung Webshop-Redesign', 'a1000000-0000-0000-0000-000000000001', '2026-01-20 14:00:00'),
('b1000000-0000-0000-0000-000000000002', 'MA-2026-001', 'MVP-Umfangsschätzung',           'a1000000-0000-0000-0000-000000000002', '2026-02-05 11:00:00');

-- ── Webshop v1 (submitted) ────────────────────────────────────────────────────
-- Params: Tagessatz=900, StdAbw=2.0, Vertrieb=0.12, Driver QA=0.15
-- Calculated: riskFactor≈0.09, driverFactor=0.15 → offerPT = mean * 1.24
-- cost = offerPT * 900, offerPrice = cost * 1.12

INSERT INTO submitted_estimation_versions (id, version_number, total_effort, notes, estimation_id, submitted_at, created_at) VALUES
('c1000000-0000-0000-0000-000000000001', 1, 65.0, 'Vollständiger Umfang inklusive aller Phasen', 'b1000000-0000-0000-0000-000000000001', '2026-01-25 17:00:00', '2026-01-20 14:00:00');

INSERT INTO submitted_estimation_parameters (id, name, param_value, comment, version_id) VALUES
('d1000000-0000-0000-0000-000000000001', 'Tagessatz',                  900.0, NULL, 'c1000000-0000-0000-0000-000000000001'),
('d1000000-0000-0000-0000-000000000002', 'Standardabweichungsfaktor',  2.0,   NULL, 'c1000000-0000-0000-0000-000000000001'),
('d1000000-0000-0000-0000-000000000003', 'Vertriebszuschlag',          0.12,  NULL, 'c1000000-0000-0000-0000-000000000001');

INSERT INTO submitted_effort_drivers (id, description, factor, comment, version_id) VALUES
('da000000-0000-0000-0000-000000000001', 'Qualitätssicherung (QA)', 0.15, 'Inkl. automatisierter Tests', 'c1000000-0000-0000-0000-000000000001');

INSERT INTO submitted_project_phases (id, name, abbreviation, duration_weeks, version_id) VALUES
('e1000000-0000-0000-0000-000000000001', 'Konzeption', 'KO', 3.0,  'c1000000-0000-0000-0000-000000000001'),
('e1000000-0000-0000-0000-000000000002', 'Umsetzung',  'UM', 12.0, 'c1000000-0000-0000-0000-000000000001'),
('e1000000-0000-0000-0000-000000000003', 'Abnahme',    'AB', 2.0,  'c1000000-0000-0000-0000-000000000001');

-- Groups: no phase_abbreviation column (removed in V5)
INSERT INTO submitted_estimation_item_groups (id, logical_id, title, version_id) VALUES
('f1000000-0000-0000-0000-000000000001', 'f1010000-0000-0000-0000-000000000001', 'U01: Konzeption',           'c1000000-0000-0000-0000-000000000001'),
('f1000000-0000-0000-0000-000000000002', 'f1020000-0000-0000-0000-000000000001', 'U02: Frontend Redesign',    'c1000000-0000-0000-0000-000000000001'),
('f1000000-0000-0000-0000-000000000003', 'f1030000-0000-0000-0000-000000000001', 'U03: Backend & Datenbank',  'c1000000-0000-0000-0000-000000000001'),
('f1000000-0000-0000-0000-000000000004', 'f1040000-0000-0000-0000-000000000001', 'U04: Abnahme & Go-live',    'c1000000-0000-0000-0000-000000000001');

-- Items: phase_abbreviation per item (added in V5)
INSERT INTO submitted_estimation_items (id, logical_id, item_type, description, min_effort, expected_effort, max_effort, mean, variance, risk_surcharge, driver_surcharge, offer_pt, cost, offer_price, phase_abbreviation, group_id) VALUES
-- U01 Konzeption → phase KO
('aa000000-0000-0000-0000-000000000001', 'aa010000-0000-0000-0000-000000000001', 'FIXED', 'Anforderungsworkshop & Kickoff',              1.0, 2.0,  3.0,  2.0,  0.11, 0.18, 0.30, 2.48,  2232.0,  2500.0, 'KO', 'f1000000-0000-0000-0000-000000000001'),
('aa000000-0000-0000-0000-000000000002', 'aa010000-0000-0000-0000-000000000002', 'FIXED', 'Systemarchitektur & Tech-Stack-Entscheidung', 2.0, 3.0,  5.0,  3.17, 0.25, 0.29, 0.48, 3.94,  3546.0,  3972.0, 'KO', 'f1000000-0000-0000-0000-000000000001'),
('aa000000-0000-0000-0000-000000000003', 'aa010000-0000-0000-0000-000000000003', 'FIXED', 'Datenbankdesign & ER-Modell',                 1.0, 2.0,  4.0,  2.17, 0.25, 0.20, 0.33, 2.70,  2430.0,  2722.0, 'KO', 'f1000000-0000-0000-0000-000000000001'),
-- U02 Frontend → phase UM
('aa000000-0000-0000-0000-000000000004', 'aa020000-0000-0000-0000-000000000001', 'FIXED', 'Produktlisting & Suchfunktion',               3.0, 5.0,  8.0,  5.17, 0.69, 0.47, 0.78, 6.42,  5778.0,  6472.0, 'UM', 'f1000000-0000-0000-0000-000000000002'),
('aa000000-0000-0000-0000-000000000005', 'aa020000-0000-0000-0000-000000000002', 'FIXED', 'Warenkorb & Checkout-Prozess',                5.0, 8.0,  12.0, 8.17, 1.36, 0.74, 1.23, 10.14, 9126.0,  10221.0, 'UM', 'f1000000-0000-0000-0000-000000000002'),
('aa000000-0000-0000-0000-000000000006', 'aa020000-0000-0000-0000-000000000003', 'FIXED', 'Benutzerkonto & Login',                       2.0, 4.0,  6.0,  4.0,  0.44, 0.36, 0.60, 4.96,  4464.0,  4999.0, 'UM', 'f1000000-0000-0000-0000-000000000002'),
('aa000000-0000-0000-0000-000000000007', 'aa020000-0000-0000-0000-000000000004', 'FIXED', 'Responsive Design & Mobile Optimierung',      2.0, 3.0,  5.0,  3.17, 0.25, 0.29, 0.48, 3.94,  3546.0,  3972.0, 'UM', 'f1000000-0000-0000-0000-000000000002'),
-- U03 Backend → phase UM
('aa000000-0000-0000-0000-000000000008', 'aa030000-0000-0000-0000-000000000001', 'FIXED', 'REST API Endpoints (CRUD)',                   4.0, 6.0,  9.0,  6.17, 0.69, 0.56, 0.93, 7.66,  6894.0,  7721.0, 'UM', 'f1000000-0000-0000-0000-000000000003'),
('aa000000-0000-0000-0000-000000000009', 'aa030000-0000-0000-0000-000000000002', 'FIXED', 'Authentifizierung & Autorisierung',           2.0, 4.0,  6.0,  4.0,  0.44, 0.36, 0.60, 4.96,  4464.0,  4999.0, 'UM', 'f1000000-0000-0000-0000-000000000003'),
('aa000000-0000-0000-0000-000000000010', 'aa030000-0000-0000-0000-000000000003', 'FIXED', 'Datenbankmigrationen & Seeding',              1.0, 2.0,  3.0,  2.0,  0.11, 0.18, 0.30, 2.48,  2232.0,  2500.0, 'UM', 'f1000000-0000-0000-0000-000000000003'),
('aa000000-0000-0000-0000-000000000011', 'aa030000-0000-0000-0000-000000000004', 'FIXED', 'Payment-Integration (Stripe)',                3.0, 5.0,  8.0,  5.17, 0.69, 0.47, 0.78, 6.42,  5778.0,  6472.0, 'UM', 'f1000000-0000-0000-0000-000000000003'),
-- U04 Abnahme → phase AB
('aa000000-0000-0000-0000-000000000012', 'aa040000-0000-0000-0000-000000000001', 'FIXED', 'Integrationstests & E2E-Tests',               2.0, 3.0,  5.0,  3.17, 0.25, 0.29, 0.48, 3.94,  3546.0,  3972.0, 'AB', 'f1000000-0000-0000-0000-000000000004'),
('aa000000-0000-0000-0000-000000000013', 'aa040000-0000-0000-0000-000000000002', 'FIXED', 'User Acceptance Testing (UAT)',               1.0, 2.0,  3.0,  2.0,  0.11, 0.18, 0.30, 2.48,  2232.0,  2500.0, 'AB', 'f1000000-0000-0000-0000-000000000004'),
('aa000000-0000-0000-0000-000000000014', 'aa040000-0000-0000-0000-000000000003', 'FIXED', 'Go-live, Deployment & Monitoring-Setup',      1.0, 2.0,  3.0,  2.0,  0.11, 0.18, 0.30, 2.48,  2232.0,  2500.0, 'AB', 'f1000000-0000-0000-0000-000000000004');

INSERT INTO submitted_additional_costs (id, description, amount, type, amount_per_week, phase_abbreviation, version_id) VALUES
('ac000000-0000-0000-0000-000000000001', 'Software-Lizenzen (Figma, JIRA)',  3500.0, 'ONE_TIME',  NULL,  'KO', 'c1000000-0000-0000-0000-000000000001'),
('ac000000-0000-0000-0000-000000000002', 'Hosting & Infrastruktur (AWS)',    0.0,    'RECURRING', 250.0, 'UM', 'c1000000-0000-0000-0000-000000000001');

-- ── Webshop v2 (draft) ────────────────────────────────────────────────────────
-- Same structure, slightly revised estimates; two drivers (QA + Komplexität)
-- Items have phase_id FK → draft_project_phases rows

INSERT INTO draft_estimation_versions (id, version_number, notes, estimation_id, created_at) VALUES
('c1000000-0000-0000-0000-000000000002', 2, 'Scope nach Kunden-Feedback angepasst — Mobile-Optimierung ersetzt durch UX-Konzept', 'b1000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00');

INSERT INTO draft_estimation_parameters (id, name, param_value, comment, version_id) VALUES
('d1000000-0000-0000-0000-000000000004', 'Tagessatz',                 900.0, NULL, 'c1000000-0000-0000-0000-000000000002'),
('d1000000-0000-0000-0000-000000000005', 'Standardabweichungsfaktor', 2.0,   NULL, 'c1000000-0000-0000-0000-000000000002'),
('d1000000-0000-0000-0000-000000000006', 'Vertriebszuschlag',         0.12,  NULL, 'c1000000-0000-0000-0000-000000000002');

INSERT INTO draft_effort_drivers (id, description, factor, comment, version_id) VALUES
('db000000-0000-0000-0000-000000000001', 'Qualitätssicherung (QA)',  0.15, 'Inkl. automatisierter Tests',       'c1000000-0000-0000-0000-000000000002'),
('db000000-0000-0000-0000-000000000002', 'Technische Komplexität',   0.10, 'Legacy-System-Anbindung (SAP)',     'c1000000-0000-0000-0000-000000000002');

INSERT INTO draft_project_phases (id, name, abbreviation, duration_weeks, version_id) VALUES
('e2000000-0000-0000-0000-000000000001', 'Konzeption', 'KO', 4.0,  'c1000000-0000-0000-0000-000000000002'),
('e2000000-0000-0000-0000-000000000002', 'Umsetzung',  'UM', 10.0, 'c1000000-0000-0000-0000-000000000002'),
('e2000000-0000-0000-0000-000000000003', 'Abnahme',    'AB', 2.0,  'c1000000-0000-0000-0000-000000000002');

-- Groups: no phase_id column (removed in V5)
INSERT INTO draft_estimation_item_groups (id, logical_id, title, version_id) VALUES
('f2000000-0000-0000-0000-000000000001', 'f1010000-0000-0000-0000-000000000001', 'U01: Konzeption',          'c1000000-0000-0000-0000-000000000002'),
('f2000000-0000-0000-0000-000000000002', 'f1020000-0000-0000-0000-000000000001', 'U02: Frontend Redesign',   'c1000000-0000-0000-0000-000000000002'),
('f2000000-0000-0000-0000-000000000003', 'f1030000-0000-0000-0000-000000000001', 'U03: Backend & Datenbank', 'c1000000-0000-0000-0000-000000000002'),
('f2000000-0000-0000-0000-000000000004', 'f1040000-0000-0000-0000-000000000001', 'U04: Abnahme & Go-live',   'c1000000-0000-0000-0000-000000000002');

-- Items with phase_id FK (item-level phase assignment, added in V5)
INSERT INTO draft_estimation_items (id, logical_id, item_type, description, min_effort, expected_effort, max_effort, phase_id, group_id, created_at) VALUES
-- U01 Konzeption → KO
('ab000000-0000-0000-0000-000000000001', 'aa010000-0000-0000-0000-000000000001', 'FIXED', 'Anforderungsworkshop & Kickoff',              1.0, 2.0,  4.0,  'e2000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000002', 'aa010000-0000-0000-0000-000000000002', 'FIXED', 'Systemarchitektur & Tech-Stack-Entscheidung', 3.0, 4.0,  6.0,  'e2000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000003', 'aa010000-0000-0000-0000-000000000003', 'FIXED', 'Datenbankdesign & ER-Modell',                 1.0, 2.0,  3.0,  'e2000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000004', 'aa010000-0000-0000-0000-000000000005', 'FIXED', 'UX-Konzept & Wireframes',                     3.0, 5.0,  8.0,  'e2000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001', '2026-02-10 16:00:00'),
-- U02 Frontend → UM
('ab000000-0000-0000-0000-000000000005', 'aa020000-0000-0000-0000-000000000001', 'FIXED', 'Produktlisting & Suchfunktion',               3.0, 5.0,  7.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000006', 'aa020000-0000-0000-0000-000000000002', 'FIXED', 'Warenkorb & Checkout-Prozess',                5.0, 8.0,  13.0, 'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000007', 'aa020000-0000-0000-0000-000000000003', 'FIXED', 'Benutzerkonto & Login (OAuth2)',              2.0, 4.0,  6.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000008', 'aa020000-0000-0000-0000-000000000006', 'FIXED', 'Produkt-Detailseite & Bildergalerie',         1.0, 2.0,  4.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002', '2026-02-10 16:00:00'),
-- U03 Backend → UM
('ab000000-0000-0000-0000-000000000009', 'aa030000-0000-0000-0000-000000000001', 'FIXED', 'REST API Endpoints (CRUD)',                   4.0, 6.0,  9.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000010', 'aa030000-0000-0000-0000-000000000002', 'FIXED', 'Authentifizierung & Autorisierung',           2.0, 3.0,  5.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000011', 'aa030000-0000-0000-0000-000000000003', 'FIXED', 'Datenbankmigrationen & Seeding',              1.0, 2.0,  3.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000012', 'aa030000-0000-0000-0000-000000000004', 'FIXED', 'Payment-Integration (Stripe)',                3.0, 5.0,  8.0,  'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000013', 'aa030000-0000-0000-0000-000000000005', 'FIXED', 'E-Mail-Benachrichtigungen (Bestellung/Versand)', 1.0, 2.0, 4.0, 'e2000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000003', '2026-02-10 16:00:00'),
-- U04 Abnahme → AB
('ab000000-0000-0000-0000-000000000014', 'aa040000-0000-0000-0000-000000000001', 'FIXED', 'Integrationstests & E2E-Tests',               2.0, 3.0,  5.0,  'e2000000-0000-0000-0000-000000000003', 'f2000000-0000-0000-0000-000000000004', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000015', 'aa040000-0000-0000-0000-000000000002', 'FIXED', 'User Acceptance Testing (UAT)',               2.0, 3.0,  4.0,  'e2000000-0000-0000-0000-000000000003', 'f2000000-0000-0000-0000-000000000004', '2026-02-10 16:00:00'),
('ab000000-0000-0000-0000-000000000016', 'aa040000-0000-0000-0000-000000000003', 'FIXED', 'Go-live, Deployment & Monitoring-Setup',      1.0, 2.0,  3.0,  'e2000000-0000-0000-0000-000000000003', 'f2000000-0000-0000-0000-000000000004', '2026-02-10 16:00:00');

INSERT INTO draft_additional_costs (id, description, amount, type, amount_per_week, phase_id, version_id) VALUES
('ae000000-0000-0000-0000-000000000001', 'Software-Lizenzen (Figma, JIRA, Confluence)', 2500.0, 'ONE_TIME',  NULL,  'e2000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000002'),
('ae000000-0000-0000-0000-000000000002', 'Hosting & Infrastruktur (AWS)',               0.0,    'RECURRING', 300.0, 'e2000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002');

-- ── Mobile App v1 (submitted) ─────────────────────────────────────────────────
-- Params: Tagessatz=950, StdAbw=2.0, Vertrieb=0.15, Driver iOS+Android=0.20
-- Calculated: riskFactor≈0.077, driverFactor=0.20 → offerPT = mean * 1.277
-- cost = offerPT * 950, offerPrice = cost * 1.15

INSERT INTO submitted_estimation_versions (id, version_number, total_effort, notes, estimation_id, submitted_at, created_at) VALUES
('c2000000-0000-0000-0000-000000000001', 1, 84.0, 'Vollständiger MVP-Scope mit iOS & Android', 'b1000000-0000-0000-0000-000000000002', '2026-02-10 10:00:00', '2026-02-05 11:00:00');

INSERT INTO submitted_estimation_parameters (id, name, param_value, comment, version_id) VALUES
('d2000000-0000-0000-0000-000000000001', 'Tagessatz',                 950.0, 'Mobile-Entwickler-Rate', 'c2000000-0000-0000-0000-000000000001'),
('d2000000-0000-0000-0000-000000000002', 'Standardabweichungsfaktor', 2.0,   NULL,                     'c2000000-0000-0000-0000-000000000001'),
('d2000000-0000-0000-0000-000000000003', 'Vertriebszuschlag',         0.15,  NULL,                     'c2000000-0000-0000-0000-000000000001');

INSERT INTO submitted_effort_drivers (id, description, factor, comment, version_id) VALUES
('da000000-0000-0000-0000-000000000002', 'iOS & Android Multiplattform', 0.20, 'Native Implementierung auf beiden Plattformen', 'c2000000-0000-0000-0000-000000000001');

INSERT INTO submitted_project_phases (id, name, abbreviation, duration_weeks, version_id) VALUES
('e3000000-0000-0000-0000-000000000001', 'Konzeption & Design',   'KD', 4.0, 'c2000000-0000-0000-0000-000000000001'),
('e3000000-0000-0000-0000-000000000002', 'Sprint 1 – Grundlagen', 'S1', 6.0, 'c2000000-0000-0000-0000-000000000001'),
('e3000000-0000-0000-0000-000000000003', 'Sprint 2 – Features',   'S2', 6.0, 'c2000000-0000-0000-0000-000000000001'),
('e3000000-0000-0000-0000-000000000004', 'App Store Release',     'AS', 2.0, 'c2000000-0000-0000-0000-000000000001');

INSERT INTO submitted_estimation_item_groups (id, logical_id, title, version_id) VALUES
('f3000000-0000-0000-0000-000000000001', 'f3010000-0000-0000-0000-000000000001', 'M01: Konzeption & UX', 'c2000000-0000-0000-0000-000000000001'),
('f3000000-0000-0000-0000-000000000002', 'f3020000-0000-0000-0000-000000000001', 'M02: App Features',    'c2000000-0000-0000-0000-000000000001'),
('f3000000-0000-0000-0000-000000000003', 'f3030000-0000-0000-0000-000000000001', 'M03: Backend & API',   'c2000000-0000-0000-0000-000000000001'),
('f3000000-0000-0000-0000-000000000004', 'f3040000-0000-0000-0000-000000000001', 'M04: Release & QA',    'c2000000-0000-0000-0000-000000000001');

INSERT INTO submitted_estimation_items (id, logical_id, item_type, description, min_effort, expected_effort, max_effort, mean, variance, risk_surcharge, driver_surcharge, offer_pt, cost, offer_price, phase_abbreviation, group_id) VALUES
-- M01 KD
('ba000000-0000-0000-0000-000000000001', 'ba010000-0000-0000-0000-000000000001', 'FIXED', 'UX Research & Nutzerinterviews',          2.0, 3.0,  5.0,  3.17, 0.25, 0.24, 0.63, 4.04,  3838.0,  4414.0, 'KD', 'f3000000-0000-0000-0000-000000000001'),
('ba000000-0000-0000-0000-000000000002', 'ba010000-0000-0000-0000-000000000002', 'FIXED', 'UI-Design & Designsystem',                5.0, 8.0,  12.0, 8.17, 1.36, 0.63, 1.63, 10.43, 9909.0,  11395.0, 'KD', 'f3000000-0000-0000-0000-000000000001'),
('ba000000-0000-0000-0000-000000000003', 'ba010000-0000-0000-0000-000000000003', 'FIXED', 'App-Architektur & Projektsetup',          2.0, 3.0,  4.0,  3.0,  0.11, 0.23, 0.60, 3.83,  3639.0,  4185.0, 'KD', 'f3000000-0000-0000-0000-000000000001'),
-- M02 App Features — S1
('ba000000-0000-0000-0000-000000000004', 'ba020000-0000-0000-0000-000000000001', 'FIXED', 'Authentifizierung (Biometrie, PIN)',       3.0, 5.0,  8.0,  5.17, 0.69, 0.40, 1.03, 6.60,  6270.0,  7211.0, 'S1', 'f3000000-0000-0000-0000-000000000002'),
('ba000000-0000-0000-0000-000000000005', 'ba020000-0000-0000-0000-000000000002', 'FIXED', 'Dashboard & Kontoübersicht',               3.0, 5.0,  8.0,  5.17, 0.69, 0.40, 1.03, 6.60,  6270.0,  7211.0, 'S1', 'f3000000-0000-0000-0000-000000000002'),
('ba000000-0000-0000-0000-000000000006', 'ba020000-0000-0000-0000-000000000003', 'FIXED', 'Push-Benachrichtigungen',                  2.0, 3.0,  5.0,  3.17, 0.25, 0.24, 0.63, 4.04,  3838.0,  4414.0, 'S1', 'f3000000-0000-0000-0000-000000000002'),
-- M02 App Features — S2
('ba000000-0000-0000-0000-000000000007', 'ba020000-0000-0000-0000-000000000004', 'FIXED', 'Transaktionshistorie & Filter',            3.0, 5.0,  7.0,  5.0,  0.44, 0.39, 1.00, 6.39,  6071.0,  6982.0, 'S2', 'f3000000-0000-0000-0000-000000000002'),
('ba000000-0000-0000-0000-000000000008', 'ba020000-0000-0000-0000-000000000005', 'FIXED', 'Profil & Einstellungen',                   2.0, 3.0,  4.0,  3.0,  0.11, 0.23, 0.60, 3.83,  3639.0,  4185.0, 'S2', 'f3000000-0000-0000-0000-000000000002'),
('ba000000-0000-0000-0000-000000000009', 'ba020000-0000-0000-0000-000000000006', 'FIXED', 'Offline-Modus & Datensynchronisation',     4.0, 7.0,  10.0, 7.0,  1.00, 0.54, 1.40, 8.94,  8493.0,  9767.0, 'S2', 'f3000000-0000-0000-0000-000000000002'),
-- M03 Backend — S1
('ba000000-0000-0000-0000-000000000010', 'ba030000-0000-0000-0000-000000000001', 'FIXED', 'REST API Design & Dokumentation',          2.0, 3.0,  4.0,  3.0,  0.11, 0.23, 0.60, 3.83,  3639.0,  4185.0, 'S1', 'f3000000-0000-0000-0000-000000000003'),
('ba000000-0000-0000-0000-000000000011', 'ba030000-0000-0000-0000-000000000002', 'FIXED', 'Auth & JWT-Token-Service',                 2.0, 3.0,  5.0,  3.17, 0.25, 0.24, 0.63, 4.04,  3838.0,  4414.0, 'S1', 'f3000000-0000-0000-0000-000000000003'),
-- M03 Backend — S2
('ba000000-0000-0000-0000-000000000012', 'ba030000-0000-0000-0000-000000000003', 'FIXED', 'Daten-API & Business Logic',               4.0, 6.0,  9.0,  6.17, 0.69, 0.47, 1.23, 7.87,  7477.0,  8598.0, 'S2', 'f3000000-0000-0000-0000-000000000003'),
-- M04 AS
('ba000000-0000-0000-0000-000000000013', 'ba040000-0000-0000-0000-000000000001', 'FIXED', 'App Store Einreichung (iOS & Android)',    2.0, 3.0,  5.0,  3.17, 0.25, 0.24, 0.63, 4.04,  3838.0,  4414.0, 'AS', 'f3000000-0000-0000-0000-000000000004'),
('ba000000-0000-0000-0000-000000000014', 'ba040000-0000-0000-0000-000000000002', 'FIXED', 'Regression-Tests & Bugfixing',             3.0, 4.0,  6.0,  4.17, 0.25, 0.32, 0.83, 5.32,  5054.0,  5812.0, 'AS', 'f3000000-0000-0000-0000-000000000004'),
('ba000000-0000-0000-0000-000000000015', 'ba040000-0000-0000-0000-000000000003', 'FIXED', 'Beta-Test & Feedback-Implementierung',     2.0, 3.0,  5.0,  3.17, 0.25, 0.24, 0.63, 4.04,  3838.0,  4414.0, 'AS', 'f3000000-0000-0000-0000-000000000004');

INSERT INTO submitted_additional_costs (id, description, amount, type, amount_per_week, phase_abbreviation, version_id) VALUES
('ac000000-0000-0000-0000-000000000003', 'Apple Developer Program',    99.0,   'ONE_TIME',  NULL,  'AS', 'c2000000-0000-0000-0000-000000000001'),
('ac000000-0000-0000-0000-000000000004', 'Google Play Store Gebühr',   25.0,   'ONE_TIME',  NULL,  'AS', 'c2000000-0000-0000-0000-000000000001'),
('ac000000-0000-0000-0000-000000000005', 'Backend-Hosting (Firebase)', 0.0,    'RECURRING', 150.0, 'S1', 'c2000000-0000-0000-0000-000000000001');
