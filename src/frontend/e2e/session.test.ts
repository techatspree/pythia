import { test, expect, type APIRequestContext, type BrowserContext } from '@playwright/test';

const API = 'http://localhost:8090';
const LOCAL = 'http://localhost:5173';

// Full two-phase collaborative-session flow (task-067) driven with TWO browser
// contexts: a moderator (dev-admin) and one estimator (dev-estimator). Proves the
// socket broadcast (participant list mirrors in both), the blind PHASE1 count,
// the PHASE2 reveal with the domain-computed aggregate matching the backend
// AggregateDto, the diverged highlight, agree + finalize, the FINALIZED summary,
// and that finalize wrote the aggregated triple back onto the draft leaf.

function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

async function setUp(req: APIRequestContext, title = 'E2E Session') {
	const H = { Authorization: 'Dev dev-admin' };
	const proj = await req.post(`${API}/api/projects`, {
		headers: H,
		data: { name: 'Session Project', description: 'e2e session', client: 'Tester' }
	});
	expect(proj.status(), `POST projects: ${await proj.text()}`).toBe(201);
	const projectId = (await proj.json()).id;

	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: H,
		data: { offer: 'SESSION-001', description: 'session estimation' }
	});
	expect(est.status(), `POST estimations: ${await est.text()}`).toBe(201);
	const estimationId = (await est.json()).id;

	const draft = await req.post(`${API}/api/estimations/${estimationId}/versions`, { headers: H });
	expect(draft.status(), `POST versions: ${await draft.text()}`).toBe(201);

	const put = await req.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H,
		data: {
			dailyRate: 900,
			stdDevFactor: 2,
			roots: [
				{ type: 'FIXED', description: 'Feature A', minEffort: 1, expectedEffort: 2, maxEffort: 3 },
				{ type: 'FIXED', description: 'Feature B', minEffort: 1, expectedEffort: 2, maxEffort: 3 }
			]
		}
	});
	expect(put.status(), `PUT draft: ${await put.text()}`).toBe(200);

	const draftRes = await req.get(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H
	});
	const roots = (await draftRes.json()).roots as Array<{ logicalId: string }>;
	const leafIds = roots.map((r) => r.logicalId);

	const session = await req.post(`${API}/api/sessions`, {
		headers: H,
		data: { estimationId, title, itemLogicalIds: leafIds }
	});
	expect(session.status(), `POST sessions: ${await session.text()}`).toBe(201);
	const sessionId = (await session.json()).id;

	return { estimationId, leafIds, sessionId };
}

async function fillTriple(ctx: BrowserContext, min: string, expected: string, max: string) {
	const page = ctx.pages()[0];
	await page.getByRole('spinbutton').nth(0).fill(min);
	await page.getByRole('spinbutton').nth(1).fill(expected);
	await page.getByRole('spinbutton').nth(2).fill(max);
}

test('two-phase session: broadcast, blind count, reveal aggregate, finalize + write-back', async ({
	browser
}) => {
	const modCtx = await browser.newContext({ baseURL: LOCAL, locale: 'de-DE', storageState: seed('dev-admin') });
	const estCtx = await browser.newContext({ baseURL: LOCAL, locale: 'de-DE', storageState: seed('dev-estimator') });
	const mod = await modCtx.newPage();
	const est = await estCtx.newPage();

	try {
		const { estimationId, leafIds, sessionId } = await setUp(mod.request);

		// 1 + 2. Both open the room; the estimator auto-joins and BOTH contexts
		// show two participants (proves the socket broadcast).
		await mod.goto(`/sessions/${sessionId}`);
		await est.goto(`/sessions/${sessionId}`);
		await expect(mod.getByTestId('participant')).toHaveCount(2);
		await expect(est.getByTestId('participant')).toHaveCount(2);

		// Moderator starts the session → both move to PHASE1.
		await mod.getByRole('button', { name: 'Sitzung starten' }).click();
		await expect(mod.getByRole('button', { name: 'Schätzung abgeben' })).toBeVisible();
		await expect(est.getByRole('button', { name: 'Schätzung abgeben' })).toBeVisible();

		// 3. Both submit blind PHASE1 triples (divergent, to trigger the highlight).
		await fillTriple(estCtx, '2', '4', '6');
		await est.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await fillTriple(modCtx, '8', '12', '20');
		await mod.getByRole('button', { name: 'Schätzung abgeben' }).click();

		// Moderator sees the count reach 2/2 but NO values/aggregate in PHASE1.
		await expect(mod.getByTestId('phase1-count')).toContainText('2');
		await expect(mod.getByTestId('aggregate')).toHaveCount(0);

		// 4. Moderator reveals → both see the votes table + aggregate; the displayed
		// mean must equal the backend AggregateDto.
		await mod.getByRole('button', { name: 'Zu Phase 2 (aufdecken)' }).click();
		await expect(mod.getByTestId('aggregate')).toBeVisible();
		await expect(est.getByTestId('aggregate')).toBeVisible();
		await expect(est.getByTestId('diverged-banner')).toBeVisible();
			// The moderator estimates in this session, so PHASE2 gives them a revise
			// form too — the moderator can change their own vote (task-129).
			await expect(mod.getByTestId('revise-submit')).toBeVisible();

		const sRes = await mod.request.get(`${API}/api/sessions/${sessionId}`, {
			headers: { Authorization: 'Dev dev-admin' }
		});
		const agg = (await sRes.json()).items[0].aggregate;
		expect(agg.meanMin).toBe(5);
		expect(agg.meanExpected).toBe(8);
		expect(agg.meanMax).toBe(13);
		// Domain-computed display (German locale grouping) matches the backend mean.
		await expect(est.getByTestId('aggregate-mean')).toHaveText('5,0 / 8,0 / 13,0');

		// 5. Estimator revises + agrees; moderator finalizes item 1.
		await fillTriple(estCtx, '4', '8', '12');
		await est.getByRole('button', { name: 'Überarbeitung abgeben' }).click();
		await est.getByRole('button', { name: 'Ich stimme zu' }).click();
		// The agree broadcast reaches the moderator's all-agreed indicator.
		await expect(mod.getByText('Alle Schätzer haben zugestimmt.')).toBeVisible();
		await mod.getByRole('button', { name: 'Eintrag abschließen' }).click();

		// Advances to item 2 PHASE1 in both contexts.
		await expect(mod.getByText('Eintrag 2 von 2')).toBeVisible();
		await expect(est.getByRole('button', { name: 'Schätzung abgeben' })).toBeVisible();

		// Item 2: both submit, reveal, finalize → session FINALIZED.
		await fillTriple(estCtx, '3', '5', '7');
		await est.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await fillTriple(modCtx, '3', '5', '7');
		await mod.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await mod.getByRole('button', { name: 'Zu Phase 2 (aufdecken)' }).click();
		await expect(mod.getByTestId('aggregate')).toBeVisible();
		await mod.getByRole('button', { name: 'Eintrag abschließen' }).click();

		// 6. Both see the FINALIZED summary.
		await expect(mod.getByText('Sitzung abgeschlossen')).toBeVisible();
		await expect(est.getByText('Sitzung abgeschlossen')).toBeVisible();

		// The draft leaves now hold the finalized (written-back) triples. Compare
		// each leaf against the session's finalTriple rather than hardcoded numbers.
		const finalRes = await mod.request.get(`${API}/api/sessions/${sessionId}`, {
			headers: { Authorization: 'Dev dev-admin' }
		});
		const finalItems = (await finalRes.json()).items as Array<{
			nodeLogicalId: string;
			finalTriple: { minEffort: number; expectedEffort: number; maxEffort: number };
		}>;

		const draftRes = await mod.request.get(
			`${API}/api/estimations/${estimationId}/versions/draft`,
			{ headers: { Authorization: 'Dev dev-admin' } }
		);
		const draftLeaves = (await draftRes.json()).roots as Array<{
			logicalId: string;
			minEffort: number;
			expectedEffort: number;
			maxEffort: number;
		}>;

		for (const item of finalItems) {
			const leaf = draftLeaves.find((l) => l.logicalId === item.nodeLogicalId)!;
			expect(leaf.minEffort).toBeCloseTo(item.finalTriple.minEffort, 5);
			expect(leaf.expectedEffort).toBeCloseTo(item.finalTriple.expectedEffort, 5);
			expect(leaf.maxEffort).toBeCloseTo(item.finalTriple.maxEffort, 5);
		}
		// Sanity: leaf ids were the ones we ran the session over.
		expect(finalItems.map((i) => i.nodeLogicalId).sort()).toEqual([...leafIds].sort());
	} finally {
		await modCtx.close();
		await estCtx.close();
	}
});

// Pause / resume / end-early (task-144). A session that runs out of time must be
// parkable and resumable, and endable without being mislabelled as cancelled —
// while every item finalized along the way stays written back on the draft.
test('suspend parks the room, resume continues it, end-early keeps the results', async ({
	browser
}) => {
	const title = 'E2E Suspend Session';
	const modCtx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const estCtx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-estimator')
	});
	const mod = await modCtx.newPage();
	const est = await estCtx.newPage();
	const H = { Authorization: 'Dev dev-admin' };

	try {
		const { estimationId, leafIds, sessionId } = await setUp(mod.request, title);

		await mod.goto(`/sessions/${sessionId}`);
		await est.goto(`/sessions/${sessionId}`);
		await expect(mod.getByTestId('participant')).toHaveCount(2);

		await mod.getByRole('button', { name: 'Sitzung starten' }).click();
		await expect(est.getByRole('button', { name: 'Schätzung abgeben' })).toBeVisible();

		// Item 1 goes the whole way — its triple reaches the draft leaf at once.
		await fillTriple(estCtx, '2', '4', '6');
		await est.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await fillTriple(modCtx, '2', '4', '6');
		await mod.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await mod.getByRole('button', { name: 'Zu Phase 2 (aufdecken)' }).click();
		await expect(mod.getByTestId('aggregate')).toBeVisible();
		await mod.getByRole('button', { name: 'Eintrag abschließen' }).click();
		await expect(mod.getByText('Eintrag 2 von 2')).toBeVisible();

		// The clock runs out on item 2: the moderator parks the room. Both contexts
		// see the paused panel (socket broadcast) and the estimator loses the form.
		await mod.getByTestId('session-suspend').click();
		await expect(mod.getByTestId('session-suspended')).toBeVisible();
		await expect(est.getByTestId('session-suspended')).toBeVisible();
		await expect(est.getByRole('button', { name: 'Schätzung abgeben' })).toHaveCount(0);
		// The moderator's controls are moderator-only.
		await expect(est.getByTestId('session-resume')).toHaveCount(0);

		// A parked room stays discoverable — this list is the way back into it.
		await est.goto('/sessions');
		await expect(
			est.getByRole('listitem').filter({ hasText: title }).first()
		).toContainText('Pausiert');
		await est.goto(`/sessions/${sessionId}`);

		// Resume continues exactly where it stopped: item 2, PHASE1.
		await mod.getByTestId('session-resume').click();
		await expect(mod.getByText('Eintrag 2 von 2')).toBeVisible();
		await expect(est.getByRole('button', { name: 'Schätzung abgeben' })).toBeVisible();

		// End early with item 2 only half-voted.
		await fillTriple(estCtx, '3', '5', '7');
		await est.getByRole('button', { name: 'Schätzung abgeben' }).click();
		await mod.getByTestId('session-end-early').click();
		await expect(mod.getByTestId('session-ended-early')).toBeVisible();
		await expect(est.getByTestId('session-ended-early')).toBeVisible();
		// The early-ended room shows the SAME summary the FINALIZED one does.
		await expect(mod.getByText('Sitzung abgeschlossen')).toBeVisible();

		const sRes = await mod.request.get(`${API}/api/sessions/${sessionId}`, { headers: H });
		const session = await sRes.json();
		expect(session.status).toBe('ENDED_EARLY');

		const items = session.items as Array<{
			nodeLogicalId: string;
			finalTriple: { minEffort: number; expectedEffort: number; maxEffort: number } | null;
		}>;
		// Item 1 finalized; item 2 was never finalized, so it holds no final triple
		// (end-early does NOT aggregate a partially-voted item).
		expect(items[0].finalTriple).not.toBeNull();
		expect(items[1].finalTriple).toBeNull();

		const draftRes = await mod.request.get(
			`${API}/api/estimations/${estimationId}/versions/draft`,
			{ headers: H }
		);
		const leaves = (await draftRes.json()).roots as Array<{
			logicalId: string;
			minEffort: number;
			expectedEffort: number;
			maxEffort: number;
		}>;

		// The finalized item's estimate survived the early end.
		const finalized = leaves.find((l) => l.logicalId === items[0].nodeLogicalId)!;
		expect(finalized.minEffort).toBeCloseTo(items[0].finalTriple!.minEffort, 5);
		expect(finalized.expectedEffort).toBeCloseTo(items[0].finalTriple!.expectedEffort, 5);
		expect(finalized.maxEffort).toBeCloseTo(items[0].finalTriple!.maxEffort, 5);
		// The unfinalized one kept its original seeded values.
		const untouched = leaves.find((l) => l.logicalId === items[1].nodeLogicalId)!;
		expect(untouched.expectedEffort).toBeCloseTo(2, 5);
		expect(leafIds).toContain(untouched.logicalId);
	} finally {
		await modCtx.close();
		await estCtx.close();
	}
});
