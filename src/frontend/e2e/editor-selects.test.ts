import { expect, test, type APIRequestContext, type Page, type Response } from '@playwright/test';
import { AdditionalCostsPanel, EstimationGrid, PhasesPanel } from './pages';

/**
 * The four in-cell editor selects no other spec drives (task-215): the PERT
 * grid's leaf type and leaf phase, the phase duration mode, and the
 * additional-cost phase. All nine in-cell selects bind with Svelte 5 function
 * bindings; these four had no guard at all.
 *
 * Every change is proven against the BACKEND by reading the draft back through
 * the API — a select can show the new value while the model or the autosave
 * payload is wrong. Each save is awaited as the PUT itself, armed before the
 * action, never via the `editor.saved` indicator: autosave debounces 800 ms
 * and the indicator lingers 2 s, so after a second change it can still show
 * the first save.
 *
 * Self-contained: each test seeds its own PERT estimation through the API.
 */

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

/** The seeded group is row `0`, its single leaf row `0-0`. */
const LEAF = '0-0';

async function seedDraft(request: APIRequestContext): Promise<string> {
	const stamp = Date.now();
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E EditorSelects ${stamp}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-ES-${stamp}` }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			phases: [
				{ name: 'Konzeption', abbreviation: 'KO', durationWeeks: 3 },
				{ name: 'Umsetzung', abbreviation: 'UM', durationWeeks: 12 }
			],
			roots: [
				{
					type: 'GROUP',
					logicalId: crypto.randomUUID(),
					title: 'Arbeitspaket',
					children: [
						{
							type: 'FIXED',
							logicalId: crypto.randomUUID(),
							description: 'Leistung',
							minEffort: 1,
							expectedEffort: 2,
							maxEffort: 3
						}
					]
				}
			],
			additionalCosts: [
				{ description: 'Lizenz', amount: 100, type: 'ONE_TIME' },
				{ description: 'Hosting', amount: 50, amountPerWeek: 50, type: 'RECURRING' }
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

async function readDraft(request: APIRequestContext, estimationId: string) {
	const res = await request.get(`/api/estimations/${estimationId}/versions/draft`, {
		headers: AUTH
	});
	expect(res.status()).toBe(200);
	return res.json();
}

/** Arm BEFORE the action: resolves with the draft autosave PUT that follows it. */
function nextDraftSave(page: Page): Promise<Response> {
	return page.waitForResponse(
		(r) => r.request().method() === 'PUT' && r.url().includes('/versions/draft')
	);
}

async function openEditor(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
}

/**
 * Performs `action`, waits for the autosave it triggers to succeed, and returns
 * the PUT body — what the FRONTEND sent, before the backend normalised it.
 */
async function savedAfter(page: Page, action: () => Promise<void>) {
	const save = nextDraftSave(page);
	await action();
	const response = await save;
	expect(response.status()).toBe(200);
	return response.request().postDataJSON();
}

test('the grid type select persists the kind and its unit, both ways', async ({ page, request }) => {
	const estimationId = await seedDraft(request);
	const grid = new EstimationGrid(page);
	await openEditor(page, estimationId);

	// The setter changes TWO fields. The unit is asserted on the PUT body:
	// the backend defaults a TIME_RELATIVE unit to 'h/Woche' and gives a FIXED
	// leaf none, so the read-back alone could never catch a setter that
	// forgot it.
	let sent = await savedAfter(page, () => grid.setLeafType(LEAF, 'TIME_RELATIVE'));
	expect(sent.roots[0].children[0].unit).toBe('h/Woche');
	let leaf = (await readDraft(request, estimationId)).roots[0].children[0];
	expect(leaf.type).toBe('TIME_RELATIVE');
	expect(leaf.unit).toBe('h/Woche');

	sent = await savedAfter(page, () => grid.setLeafType(LEAF, 'FIXED'));
	expect(sent.roots[0].children[0].unit).toBeNull();
	leaf = (await readDraft(request, estimationId)).roots[0].children[0];
	expect(leaf.type).toBe('FIXED');
	expect(leaf.unit).toBeNull();
});

test('the grid phase select persists a phase and its removal', async ({ page, request }) => {
	const estimationId = await seedDraft(request);
	const grid = new EstimationGrid(page);
	await openEditor(page, estimationId);

	await savedAfter(page, () => grid.setLeafPhase(LEAF, 'UM'));
	expect((await readDraft(request, estimationId)).roots[0].children[0].phaseAbbreviation).toBe('UM');

	await savedAfter(page, () => grid.setLeafPhase(LEAF, ''));
	expect((await readDraft(request, estimationId)).roots[0].children[0].phaseAbbreviation).toBeNull();
});

test('the phase duration-mode select persists AUTOMATIC', async ({ page, request }) => {
	const estimationId = await seedDraft(request);
	const phases = new PhasesPanel(page);
	await openEditor(page, estimationId);
	await phases.open();

	await savedAfter(page, () => phases.setDurationMode(0, 'AUTOMATIC'));
	const ko = (await readDraft(request, estimationId)).phases.find(
		(p: { abbreviation: string }) => p.abbreviation === 'KO'
	);
	expect(ko.durationMode).toBe('AUTOMATIC');
});

test('the cost phase select persists on the one-time and on the recurring cost', async ({
	page,
	request
}) => {
	const estimationId = await seedDraft(request);
	const costs = new AdditionalCostsPanel(page);
	await openEditor(page, estimationId);
	await costs.open();

	// DOM order: the one-time table's rows, then the recurring table's.
	await savedAfter(page, () => costs.setPhase(0, 'KO'));
	await savedAfter(page, () => costs.setPhase(1, 'UM'));

	const persisted = (await readDraft(request, estimationId)).additionalCosts as {
		description: string;
		phaseAbbreviation: string | null;
	}[];
	const phaseOf = (description: string) =>
		persisted.find((c) => c.description === description)?.phaseAbbreviation;
	expect(phaseOf('Lizenz')).toBe('KO');
	expect(phaseOf('Hosting')).toBe('UM');
});

test('opening the editor and changing nothing sends no autosave', async ({ page, request }) => {
	// A function binding on a <select> writes the selected option back when its
	// getter reads `undefined`; in the editor that write would be an autosave
	// on mount. Every getter returns a defined value, and this pins it.
	const estimationId = await seedDraft(request);
	let draftPuts = 0;
	page.on('request', (r) => {
		if (r.method() === 'PUT' && r.url().includes('/versions/draft')) draftPuts++;
	});

	await openEditor(page, estimationId);
	// Well past the 800 ms autosave debounce.
	await page.waitForTimeout(2500);
	expect(draftPuts).toBe(0);
});
