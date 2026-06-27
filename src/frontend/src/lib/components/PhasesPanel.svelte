<script lang="ts">
	type Phase = { name: string; abbreviation: string; durationWeeks: number | null };
	type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

	let {
		phases,
		roots,
		calcMap,
		editable,
		onchange
	}: {
		phases: any[];
		roots: any[];
		calcMap: Map<string, CalcEntry>;
		editable: boolean;
		onchange: (phases: any[]) => void;
	} = $props();

	let items = $state<Phase[]>(
		phases.map((p) => ({ name: p.name ?? '', abbreviation: p.abbreviation ?? '', durationWeeks: p.durationWeeks ?? null }))
	);
	let open = $state(false);

	function notify() {
		onchange(items.map((i) => ({ name: i.name, abbreviation: i.abbreviation, durationWeeks: i.durationWeeks })));
	}

	function addRow() {
		items.push({ name: '', abbreviation: '', durationWeeks: null });
		notify();
	}

	function deleteRow(i: number) {
		items.splice(i, 1);
		notify();
	}

	function update(i: number, field: keyof Phase, raw: string) {
		if (field === 'durationWeeks') {
			(items[i] as any)[field] = raw === '' ? null : parseFloat(raw);
		} else {
			(items[i] as any)[field] = raw;
		}
		notify();
	}

	function collectLeaves(nodes: any[]): any[] {
		const out: any[] = [];
		for (const n of nodes) {
			if (n?.type === 'GROUP') out.push(...collectLeaves(n.children ?? []));
			else out.push(n);
		}
		return out;
	}

	function phaseOfferPT(abbr: string): { total: number; hasMissing: boolean } {
		let total = 0;
		let hasMissing = false;
		for (const leaf of collectLeaves(roots)) {
			if (leaf.phaseAbbreviation === abbr) {
				const entry = calcMap.get(leaf.logicalId);
				if (entry == null) {
					hasMissing = true;
				} else {
					total += entry.offerPT;
				}
			}
		}
		return { total, hasMissing };
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>Phases</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		{#if items.length === 0}
			{#if editable}
				<div class="p-4 text-center">
					<p class="text-sm text-gray-400 mb-3">No phases defined.</p>
					<button
						onclick={addRow}
						class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>
						Add phase
					</button>
				</div>
			{:else}
				<p class="p-4 text-sm text-gray-400 text-center">No phases</p>
			{/if}
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
						<th class="py-2 px-3 text-left">Name</th>
						<th class="py-2 px-3 text-left w-28">Abbreviation</th>
						<th class="py-2 px-3 text-right w-32">Duration (weeks)</th>
						<th class="py-2 px-3 text-right w-28">offerPT (PT)</th>
						<th class="py-2 px-3 text-right w-28">Effort/Week</th>
						{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each items as item, i (i)}
						{@const { total: totalOfferPT, hasMissing } = phaseOfferPT(item.abbreviation)}
						{@const effortPerWeek = item.durationWeeks != null && item.durationWeeks > 0 ? totalOfferPT / item.durationWeeks : null}
						<tr class="border-b hover:bg-gray-50">
							<td class="py-1 px-3">
								{#if editable}
									<input
										type="text"
										class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.name}
										oninput={(e) => update(i, 'name', e.currentTarget.value)}
									/>
								{:else}
									{item.name}
								{/if}
							</td>
							<td class="py-1 px-3">
								{#if editable}
									<input
										type="text"
										class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.abbreviation}
										oninput={(e) => update(i, 'abbreviation', e.currentTarget.value)}
									/>
								{:else}
									{item.abbreviation}
								{/if}
							</td>
							<td class="py-1 px-3 text-right">
								{#if editable}
									<input
										type="number"
										step="0.1"
										min="0"
										class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.durationWeeks ?? ''}
										oninput={(e) => update(i, 'durationWeeks', e.currentTarget.value)}
									/>
								{:else}
									<span class="tabular-nums">{item.durationWeeks ?? ''}</span>
								{/if}
							</td>
							<td class="py-1 px-3 text-right text-gray-600 tabular-nums">
								{hasMissing ? '—' : totalOfferPT.toFixed(2)}
							</td>
							<td class="py-1 px-3 text-right text-gray-600 tabular-nums">
								{effortPerWeek == null || hasMissing ? '—' : effortPerWeek.toFixed(2)}
							</td>
							{#if editable}
								<td class="py-1 px-3">
									<button
										onclick={() => deleteRow(i)}
										class="text-gray-300 hover:text-red-500 transition-colors leading-none"
										title="Delete"
									>
										✕
									</button>
								</td>
							{/if}
						</tr>
					{/each}
				</tbody>
			</table>
			{#if editable}
				<div class="p-3 border-t bg-gray-50/40">
					<button onclick={addRow} class="text-sm text-brand-green hover:text-[#007a45]">
						+ Add phase
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
