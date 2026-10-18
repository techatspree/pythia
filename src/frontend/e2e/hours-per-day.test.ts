import { expect, test, type APIRequestContext } from '@playwright/test';
import { catalogPattern, parseNumber } from './helpers';
import { ComparePage, ErrorBanner, EstimationGrid, SystemSettingsPage, VersionEditorPage } from './pages';

/**
 * Accompanying work is entered in HOURS per week and divided by the tenant's
 * hours of a working day to become person-days (task-213).
 *
 * The setting is GLOBAL — one value for the whole backend — so afterEach puts 8
 * back, even on failure. Tests within this file run one after another
 * (Playwright's `fullyParallel` is off), so they cannot race each other. Other
 * spec FILES do run in parallel while a test here holds 6: none of them asserts
 * accompanying-work numbers today; a future one that does may flake for exactly
 * this reason.
 *
 * Self-contained: every test seeds its own PERT estimation through the API.
 */

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

/** 8 / 8 / 8 h/week over a 2-week phase = 16 h; stdDevFactor 0, so offer-PT = 16 / hoursPerDay. */
const ACCOMPANYING = {
	type: 'TIME_RELATIVE',
	description: 'Projektbegleitung',
	unit: 'h/Woche',
	minEffort: 8,
	expectedEffort: 8,
	maxEffort: 8,
	phaseAbbreviation: 'KO'
};

async function setHoursPerDay(request: APIRequestContext, hoursPerDay: number) {
	const res = await request.put('/api/system/hours-per-day', {
		headers: JSON_HEADERS,
		data: { hoursPerDay }
	});
	expect(res.status()).toBe(204);
}

async function seedDraft(request: APIRequestContext, children: object[]): Promise<string> {
	const stamp = Date.now();
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E HoursPerDay ${stamp}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-HPD-${stamp}` }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	expect(
		(await request.post(`/api/estimations/${estimationId}/versions`, { headers: JSON_HEADERS })).status()
	).toBe(201);

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0,
			phases: [{ name: 'Konzeption', abbreviation: 'KO', durationWeeks: 2 }],
			roots: [{ type: 'GROUP', logicalId: crypto.randomUUID(), title: 'Arbeitspaket', children }]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

test.afterEach(async ({ request }) => {
	await setHoursPerDay(request, 8);
});

test('the tenant hours per day convert accompanying work, and an invalid value is refused', async ({
	page,
	request
}) => {
	const estimationId = await seedDraft(request, [{ ...ACCOMPANYING, logicalId: crypto.randomUUID() }]);
	const editor = new VersionEditorPage(page);

	await editor.gotoDraft(estimationId, 'draft');
	expect(parseNumber(await editor.summaryOfferPT.innerText())).toBeCloseTo(2.0, 2);
	await expect(editor.summaryHoursPerDay).toHaveText(catalogPattern('summary.hoursPerDay'));
	await expect(editor.summaryHoursPerDay).toContainText('8');

	const settings = new SystemSettingsPage(page);
	await settings.open();
	await settings.saveHoursPerDay('0');
	await new ErrorBanner(page).expectVisible();

	await settings.saveHoursPerDay('6');
	await settings.expectNotice();

	await editor.gotoDraft(estimationId, 'draft');
	expect(parseNumber(await editor.summaryOfferPT.innerText())).toBeCloseTo(16 / 6, 2);
	await expect(editor.summaryHoursPerDay).toContainText('6');
});

test('hour values are marked as hours, person-day values are not', async ({ page, request }) => {
	const estimationId = await seedDraft(request, [
		{ ...ACCOMPANYING, logicalId: crypto.randomUUID() },
		{
			type: 'FIXED',
			logicalId: crypto.randomUUID(),
			description: 'Architektur',
			minEffort: 1,
			expectedEffort: 2,
			maxEffort: 3,
			phaseAbbreviation: 'KO'
		}
	]);
	await new VersionEditorPage(page).gotoDraft(estimationId, 'draft');

	const grid = new EstimationGrid(page);
	// Optimistic, likely, pessimistic and the PERT cell of the accompanying work…
	await expect(grid.hourValues('0-0')).toHaveCount(4);
	await expect(grid.hourValues('0-0').first()).toHaveClass(/italic/);
	// …and nothing of the fixed item, whose values are person-days.
	await expect(grid.hourValues('0-1')).toHaveCount(0);
});

test('a submitted version keeps its basis; a continued draft uses the new one', async ({
	page,
	request
}) => {
	const estimationId = await seedDraft(request, [{ ...ACCOMPANYING, logicalId: crypto.randomUUID() }]);
	const submit = await request.post(`/api/estimations/${estimationId}/versions/draft/submit`, {
		headers: JSON_HEADERS
	});
	expect(submit.status()).toBe(200);

	await setHoursPerDay(request, 6);

	// The submitted version still says 8, and its numbers did not move.
	const editor = new VersionEditorPage(page);
	await editor.gotoSubmitted(estimationId, 1);
	await expect(editor.summaryHoursPerDay).toContainText('8');
	expect(parseNumber(await editor.summaryOfferPT.innerText())).toBeCloseTo(2.0, 2);

	// A draft continued from it is calculated with the current setting.
	const cont = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(cont.status()).toBe(201);
	await editor.gotoDraft(estimationId, 'draft');
	await expect(editor.summaryHoursPerDay).toContainText('6');
	expect(parseNumber(await editor.summaryOfferPT.innerText())).toBeCloseTo(16 / 6, 2);

	// And the comparison names the changed basis as a parameter change.
	const compare = new ComparePage(page);
	await compare.open(estimationId, 1, 'draft');
	await expect(compare.parameterChange('hoursPerDay')).toBeVisible();
});
