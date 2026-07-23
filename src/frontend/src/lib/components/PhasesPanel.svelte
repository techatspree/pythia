<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);

	type Phase = { name: string; abbreviation: string; durationWeeks: number | null };
	type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

	let {
		phases = $bindable<Phase[]>([]),
		roots,
		calcMap,
		editable
	}: {
		phases?: Phase[];
		roots: any[];
		calcMap: Map<string, CalcEntry>;
		editable: boolean;
	} = $props();

	let open = $state(false);

	function addRow() {
		phases.push({ name: '', abbreviation: '', durationWeeks: null });
	}

	function deleteRow(i: number) {
		phases.splice(i, 1);
	}

	function update(i: number, field: keyof Phase, raw: string) {
		if (field === 'durationWeeks') {
			(phases[i] as any)[field] = raw === '' ? null : parseFloat(raw);
		} else {
			(phases[i] as any)[field] = raw;
		}
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
		<span>{$_('panel.phases.title')}</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		{#if phases.length === 0}
			{#if editable}
				<div class="p-4 text-center">
					<p class="text-sm text-gray-400 mb-3">{$_('panel.phases.empty')}</p>
					<button
						onclick={addRow}
						class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>
						{$_('panel.phases.add')}
					</button>
				</div>
			{:else}
				<p class="p-4 text-sm text-gray-400 text-center">{$_('panel.phases.emptyReadonly')}</p>
			{/if}
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
						<th class="py-2 px-3 text-left">{$_('panel.phases.colName')}</th>
						<th class="py-2 px-3 text-left w-28">{$_('panel.phases.colAbbreviation')}</th>
						<th class="py-2 px-3 text-right w-32">{$_('panel.phases.colDuration')}</th>
						<th class="py-2 px-3 text-right w-28">{$_('panel.phases.colOfferPT')}</th>
						<th class="py-2 px-3 text-right w-28">{$_('panel.phases.colEffortPerWeek')}</th>
						{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each phases as item, i (i)}
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
								{hasMissing ? '—' : num(totalOfferPT, 2)}
							</td>
							<td class="py-1 px-3 text-right text-gray-600 tabular-nums">
								{effortPerWeek == null || hasMissing ? '—' : num(effortPerWeek, 2)}
							</td>
							{#if editable}
								<td class="py-1 px-3">
									<button
										onclick={() => deleteRow(i)}
										class="text-gray-300 hover:text-red-500 transition-colors leading-none"
										title={$_('common.delete')}
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
						{$_('panel.phases.addRow')}
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
