<script lang="ts">
	type Item = {
		logicalId: string;
		description: string;
		minEffort: number | null;
		expectedEffort: number | null;
		maxEffort: number | null;
		assumptions: string | null;
	};

	type Group = {
		logicalId: string;
		title: string;
		phaseAbbreviation: string | null;
		items: Item[];
	};

	type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

	let {
		version,
		editable,
		onchange,
		calcMap = new Map<string, CalcEntry>()
	}: {
		version: any;
		editable: boolean;
		onchange: (groups: Group[]) => void;
		calcMap?: Map<string, CalcEntry>;
	} = $props();

	function pert(o: number | null, m: number | null, p: number | null): number {
		return ((o ?? 0) + 4 * (m ?? 0) + (p ?? 0)) / 6;
	}

	function newId(): string {
		return crypto.randomUUID();
	}

	function initGroups(v: any): Group[] {
		return (v?.itemGroups ?? []).map((g: any) => ({
			logicalId: g.logicalId ?? newId(),
			title: g.title ?? '',
			phaseAbbreviation: g.phaseAbbreviation ?? null,
			items: (g.items ?? []).map((i: any) => ({
				logicalId: i.logicalId ?? newId(),
				description: i.description ?? '',
				minEffort: i.minEffort ?? null,
				expectedEffort: i.expectedEffort ?? null,
				maxEffort: i.maxEffort ?? null,
				assumptions: i.assumptions ?? null
			}))
		}));
	}

	let groups = $state<Group[]>(initGroups(version));
	let collapsed = $state(new Set<string>());

	const allItems = $derived(groups.flatMap((g) => g.items));
	const totalOpt = $derived(allItems.reduce((s, i) => s + (i.minEffort ?? 0), 0));
	const totalLik = $derived(allItems.reduce((s, i) => s + (i.expectedEffort ?? 0), 0));
	const totalPes = $derived(allItems.reduce((s, i) => s + (i.maxEffort ?? 0), 0));
	const totalExp = $derived(
		allItems.reduce((s, i) => s + pert(i.minEffort, i.expectedEffort, i.maxEffort), 0)
	);

	const calcEntries = $derived(Array.from(calcMap.values()));
	const totalOfferPT = $derived(calcEntries.reduce((s, v) => s + v.offerPT, 0));
	const totalCost = $derived(calcEntries.reduce((s, v) => s + v.cost, 0));
	const totalOfferPrice = $derived(calcEntries.reduce((s, v) => s + v.offerPrice, 0));

	function notify() {
		onchange(
			groups.map((g) => ({
				logicalId: g.logicalId,
				title: g.title,
				phaseAbbreviation: g.phaseAbbreviation,
				items: g.items.map((i) => ({
					logicalId: i.logicalId,
					description: i.description,
					minEffort: i.minEffort,
					expectedEffort: i.expectedEffort,
					maxEffort: i.maxEffort,
					assumptions: i.assumptions
				}))
			}))
		);
	}

	function toggle(id: string) {
		const next = new Set(collapsed);
		next.has(id) ? next.delete(id) : next.add(id);
		collapsed = next;
	}

	function addItem(gi: number) {
		groups[gi].items.push({
			logicalId: newId(),
			description: '',
			minEffort: null,
			expectedEffort: null,
			maxEffort: null,
			assumptions: null
		});
		// ensure expanded
		const next = new Set(collapsed);
		next.delete(groups[gi].logicalId);
		collapsed = next;
		notify();
	}

	function deleteItem(gi: number, ii: number) {
		groups[gi].items.splice(ii, 1);
		notify();
	}

	function addGroup() {
		groups.push({
			logicalId: newId(),
			title: 'New group',
			phaseAbbreviation: null,
			items: [
				{
					logicalId: newId(),
					description: '',
					minEffort: null,
					expectedEffort: null,
					maxEffort: null,
					assumptions: null
				}
			]
		});
		notify();
	}

	function updateField(gi: number, ii: number, field: keyof Item, raw: string) {
		const numeric: (keyof Item)[] = ['minEffort', 'expectedEffort', 'maxEffort'];
		const nullable: (keyof Item)[] = ['assumptions'];
		(groups[gi].items[ii] as any)[field] = numeric.includes(field)
			? raw === ''
				? null
				: parseFloat(raw)
			: nullable.includes(field)
				? raw || null
				: raw;
		notify();
	}

	// Flat list of [groupIdx, itemIdx] for all visible (non-collapsed) items
	function visibleItems(): [number, number][] {
		const result: [number, number][] = [];
		for (let gi = 0; gi < groups.length; gi++) {
			if (!collapsed.has(groups[gi].logicalId)) {
				for (let ii = 0; ii < groups[gi].items.length; ii++) {
					result.push([gi, ii]);
				}
			}
		}
		return result;
	}

	// Editable column indices (col 4 = PERT, cols 6/7/8 = calculated, all read-only)
	const editCols = [0, 1, 2, 3, 5];

	function focusCell(gi: number, ii: number, col: number) {
		(document.querySelector(`[data-cell="${gi}-${ii}-${col}"]`) as HTMLElement)?.focus();
	}

	function onKeyDown(e: KeyboardEvent, gi: number, ii: number, col: number) {
		const flat = visibleItems();
		const fi = flat.findIndex(([g, i]) => g === gi && i === ii);
		const ci = editCols.indexOf(col);
		const isNumeric = col === 1 || col === 2 || col === 3;

		if (e.key === 'Tab') {
			e.preventDefault();
			if (!e.shiftKey) {
				if (ci < editCols.length - 1) {
					focusCell(gi, ii, editCols[ci + 1]);
				} else if (fi < flat.length - 1) {
					const [ngi, nii] = flat[fi + 1];
					focusCell(ngi, nii, editCols[0]);
				}
			} else {
				if (ci > 0) {
					focusCell(gi, ii, editCols[ci - 1]);
				} else if (fi > 0) {
					const [pgi, pii] = flat[fi - 1];
					focusCell(pgi, pii, editCols[editCols.length - 1]);
				}
			}
		} else if (e.key === 'Enter') {
			e.preventDefault();
			if (fi < flat.length - 1) {
				const [ngi, nii] = flat[fi + 1];
				focusCell(ngi, nii, col);
			}
		} else if (e.key === 'ArrowDown') {
			e.preventDefault();
			if (fi < flat.length - 1) {
				const [ngi, nii] = flat[fi + 1];
				focusCell(ngi, nii, col);
			}
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			if (fi > 0) {
				const [pgi, pii] = flat[fi - 1];
				focusCell(pgi, pii, col);
			}
		} else if (e.key === 'ArrowRight') {
			const input = e.currentTarget as HTMLInputElement;
			const atEnd = isNumeric || input.selectionEnd === input.value.length;
			if (atEnd && ci < editCols.length - 1) {
				e.preventDefault();
				focusCell(gi, ii, editCols[ci + 1]);
			}
		} else if (e.key === 'ArrowLeft') {
			const input = e.currentTarget as HTMLInputElement;
			const atStart = isNumeric || input.selectionStart === 0;
			if (atStart && ci > 0) {
				e.preventDefault();
				focusCell(gi, ii, editCols[ci - 1]);
			}
		}
	}

	const colCount = $derived(editable ? 11 : 10);
</script>

<div class="border rounded-lg overflow-hidden">
	{#if groups.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">No items yet.</p>
			{#if editable}
				<button
					onclick={addGroup}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>Add group</button
				>
			{/if}
		</div>
	{:else}
		<table class="w-full text-sm border-collapse">
			<thead>
				<tr class="bg-brand-green/10 border-b text-left text-xs text-brand-green uppercase tracking-wide">
					<th class="py-2 px-3 w-6"></th>
					<th class="py-2 px-3">Description</th>
					<th class="py-2 px-2 text-right w-24">Optimistic</th>
					<th class="py-2 px-2 text-right w-24">Likely</th>
					<th class="py-2 px-2 text-right w-24">Pessimistic</th>
					<th class="py-2 px-2 text-right w-24">PERT</th>
					<th class="py-2 px-3">Assumptions</th>
					<th class="py-2 px-2 text-right w-24">offerPT (PT)</th>
					<th class="py-2 px-2 text-right w-28">Cost (EUR)</th>
					<th class="py-2 px-2 text-right w-28">Offer Price (EUR)</th>
					{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
				</tr>
			</thead>
			<tbody>
				{#each groups as group, gi}
					<!-- Group header -->
					<tr
						class="bg-gray-100 border-b cursor-pointer select-none hover:bg-gray-200"
						onclick={() => toggle(group.logicalId)}
					>
						<td class="py-2 px-3 text-gray-400 text-xs"
							>{collapsed.has(group.logicalId) ? '▶' : '▼'}</td
						>
						<td class="py-2 px-3 font-semibold text-gray-700" colspan={colCount - 1}>
							{group.title}
							<span class="ml-2 text-xs font-normal text-gray-400"
								>{group.items.length} item{group.items.length !== 1 ? 's' : ''}</span
							>
						</td>
					</tr>

					{#if !collapsed.has(group.logicalId)}
						{#each group.items as item, ii}
							{@const calc = calcMap.get(item.logicalId)}
							<tr class="border-b hover:bg-gray-50">
								<td class="py-1 px-3"></td>

								<!-- Description -->
								<td class="py-1 px-2">
									{#if editable}
										<input
											type="text"
											class="w-full bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={item.description}
											data-cell="{gi}-{ii}-0"
											oninput={(e) => updateField(gi, ii, 'description', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, gi, ii, 0)}
										/>
									{:else}
										<span class="px-1">{item.description}</span>
									{/if}
								</td>

								<!-- Optimistic -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.5"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={item.minEffort ?? ''}
											data-cell="{gi}-{ii}-1"
											oninput={(e) => updateField(gi, ii, 'minEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, gi, ii, 1)}
										/>
									{:else}
										<span class="tabular-nums">{item.minEffort ?? ''}</span>
									{/if}
								</td>

								<!-- Likely -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.5"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={item.expectedEffort ?? ''}
											data-cell="{gi}-{ii}-2"
											oninput={(e) => updateField(gi, ii, 'expectedEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, gi, ii, 2)}
										/>
									{:else}
										<span class="tabular-nums">{item.expectedEffort ?? ''}</span>
									{/if}
								</td>

								<!-- Pessimistic -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.5"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={item.maxEffort ?? ''}
											data-cell="{gi}-{ii}-3"
											oninput={(e) => updateField(gi, ii, 'maxEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, gi, ii, 3)}
										/>
									{:else}
										<span class="tabular-nums">{item.maxEffort ?? ''}</span>
									{/if}
								</td>

								<!-- PERT (always read-only) -->
								<td class="py-1 px-3 text-right text-brand-green tabular-nums">
									{pert(item.minEffort, item.expectedEffort, item.maxEffort).toFixed(2)}
								</td>

								<!-- Assumptions -->
								<td class="py-1 px-2">
									{#if editable}
										<input
											type="text"
											class="w-full bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={item.assumptions ?? ''}
											placeholder="…"
											data-cell="{gi}-{ii}-5"
											oninput={(e) =>
												updateField(gi, ii, 'assumptions', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, gi, ii, 5)}
										/>
									{:else}
										<span class="px-1 text-gray-500">{item.assumptions ?? ''}</span>
									{/if}
								</td>

								<!-- offerPT (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.offerPT.toFixed(2) : '—'}
								</td>

								<!-- Cost (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.cost.toFixed(0) : '—'}
								</td>

								<!-- Offer Price (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.offerPrice.toFixed(0) : '—'}
								</td>

								{#if editable}
									<td class="py-1 px-3">
										<button
											onclick={() => deleteItem(gi, ii)}
											class="text-gray-300 hover:text-red-500 transition-colors leading-none"
											title="Delete item"
										>
											✕
										</button>
									</td>
								{/if}
							</tr>
						{/each}

						{#if editable}
							<tr class="border-b bg-gray-50/40">
								<td colspan={colCount} class="py-1 px-11">
									<button
										onclick={() => addItem(gi)}
										class="text-xs text-brand-green hover:text-[#007a45]"
									>
										+ Add item
									</button>
								</td>
							</tr>
						{/if}
					{/if}
				{/each}

				<!-- Footer totals -->
				<tr class="bg-gray-50 border-t-2 border-gray-300 font-semibold text-sm">
					<td class="py-2 px-3"></td>
					<td class="py-2 px-3 text-xs text-gray-400 uppercase tracking-wide">Total</td>
					<td class="py-2 px-2 text-right tabular-nums">{totalOpt.toFixed(2)}</td>
					<td class="py-2 px-2 text-right tabular-nums">{totalLik.toFixed(2)}</td>
					<td class="py-2 px-2 text-right tabular-nums">{totalPes.toFixed(2)}</td>
					<td class="py-2 px-2 text-right text-brand-green tabular-nums">{totalExp.toFixed(2)}</td>
					<td class="py-2 px-3"></td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPT.toFixed(2)}</td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalCost.toFixed(0)}</td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums"
						>{totalOfferPrice.toFixed(0)}</td
					>
					{#if editable}<td class="py-2 px-3"></td>{/if}
				</tr>
			</tbody>
		</table>

		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button onclick={addGroup} class="text-sm text-brand-green hover:text-[#007a45]">
					+ Add group
				</button>
			</div>
		{/if}
	{/if}
</div>
