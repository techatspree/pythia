import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

/**
 * The bucket + sampled editor's HIERARCHY view — rendering AND creation.
 *
 * The view could always *render* an arbitrarily deep group tree (the Merlin
 * importer, task-131, produces exactly that shape) but until task-150 it could
 * not *create* one: the footer offered only `bucket.addItemRow`, and the per-row
 * `+ Gruppe` / `+ Element` actions in `hierarchyActions` render only
 * `{#if node.type === 'GROUP'}`. With no group there was no group row, and with
 * no group row there was no way to make one — and the seeded bucket estimation
 * was flat too, so the dead end never showed up in dev.
 *
 * task-150 added a root-level `bucket.addGroupRow` to the footer and the empty
 * state, and gave the seed a group. These tests pin all three halves: the
 * nesting renders, a GROUP row exposes the actions (and a leaf row does not),
 * and a flat draft can grow its first group and then use it.
 */

test.use({ viewport: { width: 1400, height: 1000 } });

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

const row = (id: string) => `[data-testid="tt-row-${id}"]`;
const childRows = (id: string) =>
	`${row(id)} > [aria-label="Children"] > [data-testid^="tt-row-"]`;

async function createBucketDraft(
	request: APIRequestContext,
	label: string
): Promise<{ estimationId: string; bucketId: string }> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E BucketHierarchy ${label} ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-BH-${label}-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	return { estimationId, bucketId: crypto.randomUUID() };
}

/** GROUP > (GROUP > 2 leaves) + 1 leaf — three levels, the Merlin-import shape. */
async function seedHierarchical(request: APIRequestContext) {
	const { estimationId, bucketId } = await createBucketDraft(request, 'nested');
	const outerId = crypto.randomUUID();
	const innerId = crypto.randomUUID();
	const deepLeafId = crypto.randomUUID();
	const deepLeaf2Id = crypto.randomUUID();
	const midLeafId = crypto.randomUUID();

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			buckets: [{ id: bucketId, position: 0, label: 'M' }],
			roots: [
				{
					type: 'GROUP',
					logicalId: outerId,
					title: 'WBS 1 Outer',
					children: [
						{
							type: 'GROUP',
							logicalId: innerId,
							title: 'WBS 1.1 Inner',
							children: [
								{
									type: 'BUCKETED',
									logicalId: deepLeafId,
									description: 'Deep sample',
									bucketId,
									isSample: true,
									minEffort: 1,
									expectedEffort: 2,
									maxEffort: 3
								},
								{
									type: 'BUCKETED',
									logicalId: deepLeaf2Id,
									description: 'Deep non-sample',
									bucketId,
									isSample: false
								}
							]
						},
						{
							type: 'BUCKETED',
							logicalId: midLeafId,
							description: 'Mid leaf',
							bucketId,
							isSample: false
						}
					]
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return { estimationId, outerId, innerId, deepLeafId, midLeafId };
}

/** Three root leaves, no group — the shape `TestDataSeeder` produces. */
async function seedFlat(request: APIRequestContext) {
	const { estimationId, bucketId } = await createBucketDraft(request, 'flat');
	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			buckets: [{ id: bucketId, position: 0, label: 'M' }],
			roots: [1, 2, 3].map((n) => ({
				type: 'BUCKETED',
				logicalId: crypto.randomUUID(),
				description: `Flat leaf ${n}`,
				bucketId,
				isSample: n === 1,
				...(n === 1 ? { minEffort: 1, expectedEffort: 2, maxEffort: 3 } : {})
			}))
		}
	});
	expect(putRes.status()).toBe(200);
	return { estimationId };
}

async function openHierarchyView(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('bucket-view-toggle-hierarchy')).toBeVisible();
	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await expect(page.getByTestId('bucket-view-toggle-hierarchy')).toHaveAttribute(
		'aria-pressed',
		'true'
	);
}

test('a hierarchical bucket draft renders all three levels in the hierarchy view', async ({
	page
}) => {
	const { estimationId, outerId, innerId, deepLeafId, midLeafId } = await seedHierarchical(
		page.request
	);
	await openHierarchyView(page, estimationId);

	// Level 1 → 2: the outer group holds the inner group plus one leaf.
	await expect(page.locator(row(outerId))).toBeVisible();
	await expect(page.locator(childRows(outerId))).toHaveCount(2);

	// Level 2 → 3: the inner group holds both deep leaves.
	await expect(page.locator(row(innerId))).toBeVisible();
	await expect(page.locator(childRows(innerId))).toHaveCount(2);

	// The deepest leaf is genuinely rendered, not merely present in the model.
	// Descriptions are editable <input>s on a draft, so assert the VALUE —
	// getByText matches text nodes and would never see it.
	await expect(page.locator(row(deepLeafId))).toBeVisible();
	await expect(page.locator(row(midLeafId))).toBeVisible();
	await expect(page.locator(`${row(deepLeafId)} input[type="text"]`).first()).toHaveValue(
		'Deep sample'
	);
});

test('a GROUP row exposes the + Gruppe / + Element actions', async ({ page }) => {
	const { estimationId, outerId, deepLeafId } = await seedHierarchical(page.request);
	await openHierarchyView(page, estimationId);

	// Present on a group. `.first()` is required, not lazy: a group row CONTAINS
	// its descendant rows in the DOM, so an unscoped lookup inside the outer row
	// also matches the inner group's buttons. DOM order puts a row's own actions
	// before its children, so `.first()` is the outer group's own button.
	const groupRow = page.locator(row(outerId));
	await expect(
		groupRow.getByRole('button', { name: '+ Gruppe', exact: true }).first()
	).toBeVisible();
	await expect(
		groupRow.getByRole('button', { name: '+ Element', exact: true }).first()
	).toBeVisible();

	// …and deliberately absent on a leaf: `hierarchyActions` gates both on
	// `node.type === 'GROUP'`. That is the chicken-and-egg the next test pins.
	const leafRow = page.locator(row(deepLeafId));
	await expect(leafRow.getByRole('button', { name: '+ Gruppe', exact: true })).toHaveCount(0);
});

// The bug this spec was written to characterise (task-150): a flat draft had no
// GROUP row, so the per-row `+ Gruppe` action could never appear, and the footer
// offered only "+ Element hinzufügen" — the first group was uncreatable. The
// root-level affordance now exists, so this asserts the whole loop, not just
// that a button renders.
test('a flat bucket draft can grow its first group, and that group is then usable', async ({
	page
}) => {
	const { estimationId } = await seedFlat(page.request);
	await openHierarchyView(page, estimationId);

	const addGroup = page.getByRole('button', { name: '+ Gruppe hinzufügen', exact: true });
	const addItem = page.getByRole('button', { name: '+ Element hinzufügen', exact: true });
	await expect(addItem).toBeVisible();
	await expect(addGroup).toBeVisible();

	// A flat draft has no group row: nothing carries the group-title input.
	const groupRow = page
		.locator('[data-testid^="tt-row-"]')
		.filter({ has: page.locator('input[placeholder="Gruppentitel…"]') });
	await expect(groupRow).toHaveCount(0);

	const allRows = page.locator('[data-testid^="tt-row-"]');
	const before = await allRows.count();

	await addGroup.click();

	// The new group is a real row, identified by its title placeholder.
	await expect(allRows).toHaveCount(before + 1);
	await expect(groupRow).toHaveCount(1);

	// The previously unreachable per-row actions are now reachable on it — the
	// loop that was impossible before. `.first()` because a group row contains
	// its descendants, so an unscoped lookup would also match theirs.
	const addChildItem = groupRow.getByRole('button', { name: '+ Element', exact: true }).first();
	await expect(addChildItem).toBeVisible();
	await expect(
		groupRow.getByRole('button', { name: '+ Gruppe', exact: true }).first()
	).toBeVisible();

	// Adding a child through that action puts a row inside the group's zone.
	await addChildItem.click();
	await expect(groupRow.locator('[aria-label="Children"] > [data-testid^="tt-row-"]')).toHaveCount(
		1
	);
});
