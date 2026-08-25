import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

/**
 * The bucket + sampled editor's two views (task-132): a per-bucket projection
 * with drag-and-drop re-bucketing, and the classical nested-group hierarchy.
 * Both are projections of ONE model, which is what these tests pin — including
 * the regression that nested leaves used to be invisible (the shape the Merlin
 * importer produces).
 */

// The editor stacks four panels above the table, so the rows we drag sit low on
// the page. page.mouse works in VIEWPORT coordinates, so a row below the fold
// cannot be grabbed at all — give this spec a tall viewport (the helper also
// scrolls the row into view).
test.use({ viewport: { width: 1400, height: 1000 } });

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

type Seeded = {
	estimationId: string;
	b1: string;
	b2: string;
	/** Deliberately left EMPTY — an empty bucket must still be a drop target. */
	b3: string;
	groupId: string;
	leafC: string;
	leafB: string;
};

/** Project + BUCKET_SAMPLED_PERT estimation + a draft whose roots nest two
 *  bucketed leaves inside a GROUP, plus one root-level leaf. */
async function seed(request: APIRequestContext): Promise<Seeded> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E BucketViews ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-BV-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	const b1 = crypto.randomUUID();
	const b2 = crypto.randomUUID();
	const b3 = crypto.randomUUID();
	const groupId = crypto.randomUUID();
	const leafA = crypto.randomUUID();
	const leafB = crypto.randomUUID();
	const leafC = crypto.randomUUID();

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			buckets: [
				{ id: b1, position: 0, label: 'Frontend' },
				{ id: b2, position: 1, label: 'Backend' },
				{ id: b3, position: 2, label: 'Untouched' }
			],
			roots: [
				{
					type: 'GROUP',
					logicalId: groupId,
					title: 'WBS 1',
					children: [
						{
							type: 'BUCKETED',
							logicalId: leafA,
							description: 'Nested sample',
							bucketId: b1,
							isSample: true,
							minEffort: 1.0,
							expectedEffort: 2.0,
							maxEffort: 3.0
						},
						{
							type: 'BUCKETED',
							logicalId: leafB,
							description: 'Nested backend item',
							bucketId: b2,
							isSample: false
						}
					]
				},
				{
					type: 'BUCKETED',
					logicalId: leafC,
					description: 'Root level item',
					bucketId: b1,
					isSample: false
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);

	return { estimationId, b1, b2, b3, groupId, leafC, leafB };
}

async function openDraft(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('bucket-view-toggle-bucket')).toBeVisible();
}

const row = (id: string) => `[data-testid="tt-row-${id}"]`;

/**
 * How long `settleAndDrop` chases a moving drop target before giving up, and
 * how long it waits between re-reads. A deadline rather than a pass count, so
 * the budget does not shrink when the machine is loaded — see the note on
 * `settleAndDrop`.
 */
const SETTLE_BUDGET_MS = 8_000;
const SETTLE_POLL_MS = 180;

/**
 * Steer the cursor onto a drop ZONE that MOVES while the drag is in flight, and
 * release only once it has stopped moving.
 *
 * For zone CONTAINERS only. A sibling row inside the list being reordered is
 * animated continuously by svelte-dnd-action and never satisfies the
 * convergence check below (measured: it fails 4 times out of 4), which is why
 * every drag in this spec aims at a zone rather than at a neighbouring row.
 *
 * Lifting a leaf out of its bucket empties that subtree, so everything below it
 * reflows upward — measured at ~244 px, one leaf row plus list padding. Reading
 * the target's box, moving, then releasing therefore races a reflow the move
 * itself triggers, and because TreeTable's zones detect by CURSOR position
 * (`useCursorForDetection`) a target that slides out from under a stationary
 * pointer takes the drop with it. A fixed pass count made that a ~1-in-3 flake,
 * locally and on CI, with the release landing on nothing.
 *
 * Waiting for two consecutive reads to agree means the last move landed
 * somewhere that then stayed put — the pointer is provably over the settled
 * target at mouse-up. `point` re-reads the geometry on every pass; returning
 * null means the target disappeared, which is a failure, not a reason to
 * release wherever the cursor happens to be.
 *
 * The convergence CHECK is what makes the drop correct; the BUDGET below is
 * only how long we are willing to wait for it. Those are different things, and
 * the budget used to be a fixed 12 passes (~2.2 s) — enough locally, but under
 * four parallel workers sharing one dev server a reflow that normally settles
 * in ~400 ms can exceed it, and the drag then failed as "never stopped moving"
 * having done nothing wrong. Observed as a ~1-in-6 full-suite flake that always
 * passed when the spec was run alone.
 *
 * So the budget is a DEADLINE, not a pass count: a loaded machine simply takes
 * more passes. It stays well inside the 30 s test timeout, and a genuinely
 * stuck target still fails loudly — with the elapsed budget and the last
 * observed drift in the message, which a bare pass count never told us.
 */
async function settleAndDrop(
	page: Page,
	point: () => Promise<{ x: number; y: number } | null>,
	what: string
): Promise<void> {
	const startedAt = Date.now();
	const deadline = startedAt + SETTLE_BUDGET_MS;
	let previous: { x: number; y: number } | null = null;
	let stable = 0;
	let drift = Number.NaN;
	let passes = 0;

	while (stable < 2 && Date.now() < deadline) {
		const target = await point();
		if (!target) throw new Error(`${what} vanished mid-drag`);
		drift = previous
			? Math.max(Math.abs(previous.x - target.x), Math.abs(previous.y - target.y))
			: Number.NaN;
		stable = previous && drift < 1 ? stable + 1 : 0;
		await page.mouse.move(target.x, target.y, { steps: 6 });
		await page.waitForTimeout(SETTLE_POLL_MS);
		previous = target;
		passes++;
	}

	// Fail loudly rather than dropping somewhere arbitrary: the fixed loops this
	// replaced turned a lost target into a silent no-op drop, which surfaced as
	// an unrelated-looking assertion failure much later.
	if (stable < 2) {
		throw new Error(
			`${what} never stopped moving: ${passes} passes over ${Date.now() - startedAt}ms, ` +
				`last drift ${drift.toFixed(1)}px (budget ${SETTLE_BUDGET_MS}ms)`
		);
	}
	await page.mouse.up();
}

/**
 * Drag a row into ANOTHER zone, by steering the cursor onto one of that zone's
 * existing child rows. Mirrors `mouseDragIntoGroup` in
 * e2e/tree-table-dnd-mouse.test.ts: TreeTable's zones use
 * `useCursorForDetection`, so the cursor has to settle over the target — and
 * the target's position must be re-read between moves because rows reflow as
 * the drag shadow is inserted and removed. Returns whether the drag engaged.
 */
async function mouseDragIntoZone(
	page: Page,
	sourceId: string,
	targetBucketId: string
): Promise<boolean> {
	await page.locator(row(sourceId)).scrollIntoViewIfNeeded();
	const handle = page.locator(`${row(sourceId)} [data-dnd-handle]`).first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error(`source row ${sourceId} not found`);

	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2);
	await page.mouse.down();
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2 + 6, { steps: 4 });
	await page.waitForTimeout(150);

	const engaged = (await page.locator('#dnd-action-dragged-el').count()) > 0;

	const zone = page.locator(`${row(`bucket:${targetBucketId}`)} > [aria-label="Children"]`);
	await settleAndDrop(
		page,
		async () => {
			const z = await zone.boundingBox();
			// Anchor near the zone's TOP, not its centre. This zone is POPULATED, and
			// svelte-dnd-action inserts a placeholder into it while hovering, growing
			// it downward — so its centre keeps sliding even once the box as a whole
			// has settled, and the release lands in the gap the placeholder vacated.
			// The top edge is pinned to the bucket row above and does not move. (The
			// empty-zone drops below keep the centre: at the `min-h-8` floor the two
			// are the same point, and those are validated as-is.)
			return z
				? { x: z.x + Math.min(z.width / 2, 400), y: z.y + Math.min(z.height / 2, 20) }
				: null;
		},
		`the drop zone of bucket ${targetBucketId}`
	);
	await page.waitForTimeout(450); // flipDurationMs + finalize microtask + margin
	return engaged;
}

/** Bucket rows start collapsed (task-134), so a test that needs to see or grab
 *  a leaf expands its bucket first. */
async function expandBucket(page: Page, bucketId: string) {
	await page.locator(`${row(`bucket:${bucketId}`)} button[aria-label="Expand"]`).first().click();
	await page.waitForTimeout(250);
}

test('bucket view groups leaves by bucket, including leaves nested in groups', async ({
	page,
	request
}) => {
	const { estimationId, b1, b2, leafC, leafB } = await seed(request);
	await openDraft(page, estimationId);

	// Bucket view is the default.
	await expect(page.getByTestId('bucket-view-toggle-bucket')).toHaveAttribute(
		'aria-pressed',
		'true'
	);

	// A row per bucket plus the trailing unassigned row (a valid drop target
	// even when empty).
	await expect(page.locator(row(`bucket:${b1}`))).toBeVisible();
	await expect(page.locator(row(`bucket:${b2}`))).toBeVisible();
	await expect(page.locator(row('bucket:unassigned'))).toBeVisible();

	// The GROUP-nested leaf appears under its bucket: the projection flattens
	// the whole tree, so nesting no longer hides items.
	await expandBucket(page, b2);
	await expandBucket(page, b1);
	await expect(page.locator(`${row(`bucket:${b2}`)} ${row(leafB)}`)).toBeVisible();
	await expect(page.locator(`${row(`bucket:${b1}`)} ${row(leafC)}`)).toBeVisible();
});

test('bucket rows are collapsed by default', async ({ page, request }) => {
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(String(e)));

	const { estimationId, b1, leafC } = await seed(request);
	await openDraft(page, estimationId);

	await expect(page.locator(row(`bucket:${b1}`))).toBeVisible();
	// The leaf rows stay in the DOM ON PURPOSE — the drop zone must exist at
	// drag start — so assert VISIBILITY, not presence.
	await expect(page.locator(row(leafC))).toHaveCount(1);
	await expect(page.locator(row(leafC))).toBeHidden();

	await expandBucket(page, b1);
	await expect(page.locator(row(leafC))).toBeVisible();

	expect(errors).toEqual([]);
});

test('a leaf can be dropped into a still-collapsed bucket', async ({ page, request }) => {
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(String(e)));

	const { estimationId, b1, b2, leafC } = await seed(request);
	await openDraft(page, estimationId);

	// Expand only the SOURCE; the target bucket stays collapsed. This is the
	// case a spring-loaded (expand-on-hover) approach could not do: the library
	// captures its drop zones at drag start, so a zone appearing mid-drag is
	// never a valid target. The zone is therefore always rendered.
	await expandBucket(page, b1);
	const select = page.locator(`${row(leafC)} select`).first();
	await expect(select).toHaveValue(b1);

	const zone = page.locator(`${row(`bucket:${b2}`)} > [aria-label="Children"]`);
	await page.locator(row(leafC)).scrollIntoViewIfNeeded();
	const handle = page.locator(`${row(leafC)} [data-dnd-handle]`).first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error('source row not found');
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2);
	await page.mouse.down();
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2 + 6, { steps: 4 });
	await page.waitForTimeout(200);
	await settleAndDrop(
		page,
		async () => {
			const z = await zone.boundingBox();
			return z ? { x: z.x + Math.min(z.width / 2, 400), y: z.y + z.height / 2 } : null;
		},
		'the bucket drop zone'
	);
	await page.waitForTimeout(700);

	await expect(select).toHaveValue(b2);
	expect(errors).toEqual([]);
});

test('dragging a leaf between buckets re-buckets it and autosaves', async ({ page, request }) => {
	const { estimationId, b1, b2, leafC } = await seed(request);
	await openDraft(page, estimationId);
	await expandBucket(page, b1);
	await expandBucket(page, b2);

	const select = page.locator(`${row(leafC)} select`).first();
	await expect(select).toHaveValue(b1);

	const savePut = page.waitForRequest(
		(r) => r.method() === 'PUT' && r.url().includes(`/estimations/${estimationId}/versions/draft`)
	);

	// Drag the Frontend leaf into the Backend bucket's zone.
	const engaged = await mouseDragIntoZone(page, leafC, b2);
	expect(engaged, 'drag should have engaged svelte-dnd-action').toBe(true);

	// The model changed: only the bucket, never the tree.
	await expect(select).toHaveValue(b2);
	await expect(page.locator(`${row(`bucket:${b2}`)} ${row(leafC)}`)).toBeVisible();

	const body = (await savePut).postDataJSON();
	const saved = JSON.stringify(body);
	expect(saved).toContain('"bucketId"');
	// The root-level leaf is still root-level in the persisted tree — the bucket
	// view never restructures.
	expect(body.roots.some((n: { logicalId?: string }) => n.logicalId === leafC)).toBe(true);
});

test('reordering bucket chips keeps every bucket', async ({ page, request }) => {
	const { estimationId } = await seed(request);
	await openDraft(page, estimationId);

	const chipNames = () =>
		page.evaluate(() =>
			Array.from(document.querySelectorAll('input'))
				.filter((i) => /Bucket/i.test(i.getAttribute('aria-label') ?? ''))
				.map((i) => (i as HTMLInputElement).value)
		);

	expect(await chipNames()).toEqual(['Frontend', 'Backend', 'Untouched']);

	// svelte-dnd-action inserts a shadow placeholder into `consider` and needs
	// that exact array back. Re-deriving the chips from the model on every
	// consider destroyed the shadow's identity, which crashed the zone and LOST
	// the dragged bucket from the panel while it stayed in the per-row select.
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(String(e)));

	const handles = page.locator('[title="Ziehen zum Sortieren"]');
	const from = await handles.nth(0).boundingBox();
	const to = await handles.nth(2).boundingBox();
	if (!from || !to) throw new Error('bucket drag handles not found');
	await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
	await page.mouse.down();
	await page.mouse.move(from.x + from.width / 2 + 8, from.y + from.height / 2, { steps: 4 });
	await page.waitForTimeout(200);
	await page.mouse.move(to.x + to.width / 2 + 20, to.y + to.height / 2, { steps: 10 });
	await page.waitForTimeout(300);
	await page.mouse.up();
	await page.waitForTimeout(600);

	// No bucket may go missing, and the move must be the one requested.
	expect(await chipNames()).toEqual(['Backend', 'Untouched', 'Frontend']);
	expect(errors, 'the drag must not throw').toEqual([]);
});

test('a leaf can be dragged into an EMPTY bucket', async ({ page, request }) => {
	const { estimationId, b1, b3, leafC } = await seed(request);
	await openDraft(page, estimationId);
	await expandBucket(page, b1);

	const select = page.locator(`${row(leafC)} select`).first();
	await expect(select).toHaveValue(b1);

	// An empty zone collapses to 0px, and TreeTable detects drops by CURSOR
	// position — so without a min-height an empty bucket can never be hovered,
	// which is the state every freshly created bucket (and a Merlin import's
	// non-"Imported" buckets) starts in.
	const zone = page.locator(`${row(`bucket:${b3}`)} > [aria-label="Children"]`);
	const zoneBox = await zone.boundingBox();
	expect(zoneBox, 'the empty bucket must render a drop zone').not.toBeNull();
	expect(zoneBox!.height, 'an empty drop zone must still be hittable').toBeGreaterThan(0);

	await page.locator(row(leafC)).scrollIntoViewIfNeeded();
	const handle = page.locator(`${row(leafC)} [data-dnd-handle]`).first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error('source row not found');
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2);
	await page.mouse.down();
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2 + 6, { steps: 4 });
	await page.waitForTimeout(150);
	await settleAndDrop(
		page,
		async () => {
			const z = await zone.boundingBox();
			return z ? { x: z.x + Math.min(z.width / 2, 400), y: z.y + z.height / 2 } : null;
		},
		'the bucket drop zone'
	);
	await page.waitForTimeout(600);

	await expect(select).toHaveValue(b3);
});

test('dropping into the unassigned row is ignored (a null bucket cannot be saved)', async ({
	page,
	request
}) => {
	const { estimationId, b1, leafC } = await seed(request);
	await openDraft(page, estimationId);
	await expandBucket(page, b1);

	const select = page.locator(`${row(leafC)} select`).first();
	await expect(select).toHaveValue(b1);

	// The unassigned row is a rescue source only: the backend rejects a BUCKETED
	// leaf with a null bucketId, so the drop must be a no-op rather than a
	// failing autosave.
	await page.locator(row('bucket:unassigned')).scrollIntoViewIfNeeded();
	const tb = await page.locator(`${row('bucket:unassigned')} > div`).first().boundingBox();
	if (!tb) throw new Error('unassigned row not found');
	await page.locator(row(leafC)).scrollIntoViewIfNeeded();
	const handle = page.locator(`${row(leafC)} [data-dnd-handle]`).first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error('source row not found');
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2);
	await page.mouse.down();
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2 + 6, { steps: 4 });
	await page.waitForTimeout(150);
	for (let i = 0; i < 4; i++) {
		const t = await page.locator(`${row('bucket:unassigned')} > div`).first().boundingBox();
		if (!t) break;
		await page.mouse.move(t.x + t.width / 2, t.y + t.height * 0.5, { steps: 6 });
		await page.waitForTimeout(180);
	}
	await page.mouse.up();
	await page.waitForTimeout(600);

	// Still in its original bucket, and no error banner appeared.
	await expect(select).toHaveValue(b1);
	await expect(page.locator(`${row(`bucket:${b1}`)} ${row(leafC)}`)).toBeVisible();
});

test('hierarchy view shows nested groups and shares the model with the bucket view', async ({
	page,
	request
}) => {
	const { estimationId, b1, groupId, leafB } = await seed(request);
	await openDraft(page, estimationId);

	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await expect(page.getByTestId('bucket-view-toggle-hierarchy')).toHaveAttribute(
		'aria-pressed',
		'true'
	);

	// The group and its nested children are visible — the regression this view
	// fixes (the old flat editor rendered the group with its items hidden).
	const groupChildren = `${row(groupId)} > [aria-label="Children"] > [data-testid^="tt-row-"]`;
	await expect(page.locator(row(groupId))).toBeVisible();
	await expect(page.locator(`${row(groupId)} ${row(leafB)}`)).toBeVisible();
	await expect(page.locator(groupChildren)).toHaveCount(2);

	// Add a child item to the group via its own row action.
	await page.locator(row(groupId)).getByRole('button', { name: '+ Element' }).first().click();
	await expect(page.locator(groupChildren)).toHaveCount(3);

	// Switching back proves both views render the same model: the new leaf was
	// created with the first bucket, so it shows up under it.
	await page.getByTestId('bucket-view-toggle-bucket').click();
	const frontendChildren = page.locator(`${row(`bucket:${b1}`)} [data-testid^="tt-row-"]`);
	await expect(frontendChildren).toHaveCount(3);
});
