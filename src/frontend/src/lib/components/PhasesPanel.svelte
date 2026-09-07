<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import DisclosureTriangle from '$lib/ui/DisclosureTriangle.svelte';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);

	type PhaseDurationMode = 'EXPLICIT' | 'AUTOMATIC';
	type Phase = {
		name: string;
		abbreviation: string;
		durationWeeks: number | null;
		durationMode?: PhaseDurationMode;
	};
	type PhaseWindow = { abbreviation: string; scheduledLeafCount: number; durationWeeks: number };
	type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

	let {
		phases = $bindable<Phase[]>([]),
		roots,
		calcMap,
		editable,
		phaseWindows = []
	}: {
		phases?: Phase[];
		roots: any[];
		calcMap: Map<string, CalcEntry>;
		editable: boolean;
		/**
		 * The span each phase occupies in the levelled plan (task-177). An
		 * AUTOMATIC phase takes its length from here; a phase with no window
		 * carries only accompanying work and has nothing to derive from.
		 */
		phaseWindows?: PhaseWindow[];
	} = $props();

	const windowOf = (abbr: string) => phaseWindows.find((w) => w.abbreviation === abbr) ?? null;

	/**
	 * Write the computed length back so the persisted number matches what is
	 * shown. Guarded: only for an AUTOMATIC phase that actually has a window,
	 * and only when the value really changed — an unconditional write would
	 * autosave on every render.
	 */
	$effect(() => {
		for (const item of phases) {
			if (item.durationMode !== 'AUTOMATIC') continue;
			const w = windowOf(item.abbreviation);
			if (w == null || w.scheduledLeafCount === 0) continue;
			const rounded = Math.round(w.durationWeeks * 100) / 100;
			if (item.durationWeeks !== rounded) item.durationWeeks = rounded;
		}
	});

	function setMode(i: number, mode: PhaseDurationMode) {
		phases[i].durationMode = mode;
	}

	let open = $state(false);

	function addRow() {
		phases.push({ name: '', abbreviation: '', durationWeeks: null, durationMode: 'EXPLICIT' });
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
		<DisclosureTriangle expanded={open} />
	</button>

	{#if open}
		{#if phases.length === 0}
			{#if editable}
				<div class="p-4 text-center">
					<p class="text-sm text-ink-faint mb-3">{$_('panel.phases.empty')}</p>
					<Button
						onclick={addRow}
						size="sm"
					>
						{$_('panel.phases.add')}
					</Button>
				</div>
			{:else}
				<p class="p-4 text-sm text-ink-faint text-center">{$_('panel.phases.emptyReadonly')}</p>
			{/if}
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-ink-muted uppercase tracking-wide">
						<th class="py-2 px-3 text-left">{$_('panel.phases.colName')}</th>
						<th class="py-2 px-3 text-left w-28">{$_('panel.phases.colAbbreviation')}</th>
						<th class="py-2 px-3 text-right w-32">{$_('panel.phases.colDuration')}</th>
						<th class="py-2 px-3 text-left w-40">{$_('panel.phases.colDurationMode')}</th>
						<th class="py-2 px-3 text-right w-28">{$_('panel.phases.colOfferPT')}</th>
						<th class="py-2 px-3 text-right w-28">{$_('panel.phases.colEffortPerWeek')}</th>
						{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each phases as item, i (i)}
						{@const { total: totalOfferPT, hasMissing } = phaseOfferPT(item.abbreviation)}
						{@const effortPerWeek = item.durationWeeks != null && item.durationWeeks > 0 ? totalOfferPT / item.durationWeeks : null}
						{@const auto = item.durationMode === 'AUTOMATIC'}
						{@const win = windowOf(item.abbreviation)}
						<tr class="border-b hover:bg-surface-subtle">
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
								{#if editable && !auto}
									<input
										type="number"
										step="0.1"
										min="0"
										class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.durationWeeks ?? ''}
										oninput={(e) => update(i, 'durationWeeks', e.currentTarget.value)}
									/>
								{:else}
									<!-- Read-only when AUTOMATIC: the number comes from the plan. -->
									<span class="tabular-nums" data-testid="phase-duration">{item.durationWeeks ?? ''}</span>
								{/if}
							</td>
							<td class="py-1 px-3">
								{#if editable}
									<select
										class="bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										data-testid="phase-duration-mode"
										value={item.durationMode ?? 'EXPLICIT'}
										onchange={(e) =>
											setMode(i, e.currentTarget.value === 'AUTOMATIC' ? 'AUTOMATIC' : 'EXPLICIT')}
									>
										<option value="EXPLICIT">{$_('panel.phases.modeExplicit')}</option>
										<option value="AUTOMATIC">{$_('panel.phases.modeAutomatic')}</option>
									</select>
								{:else}
									<span class="text-xs text-ink-muted"
										>{auto ? $_('panel.phases.modeAutomatic') : $_('panel.phases.modeExplicit')}</span
									>
								{/if}
								{#if auto && (win == null || win.scheduledLeafCount === 0)}
									<!-- Say so rather than showing zero weeks, which reads as a bug. -->
									<p class="mt-0.5 text-xs text-amber-900" data-testid="phase-no-window">
										{$_('panel.phases.noScheduledWork')}
									</p>
								{/if}
							</td>
							<td class="py-1 px-3 text-right text-ink-muted tabular-nums">
								{hasMissing ? '—' : num(totalOfferPT, 2)}
							</td>
							<td class="py-1 px-3 text-right text-ink-muted tabular-nums">
								{effortPerWeek == null || hasMissing ? '—' : num(effortPerWeek, 2)}
							</td>
							{#if editable}
								<td class="py-1 px-3">
									<button
										onclick={() => deleteRow(i)}
										class="text-ink-faint hover:text-red-500 transition-colors leading-none"
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
				<div class="p-3 border-t bg-surface-subtle/40">
					<button onclick={addRow} class="text-sm text-brand-green hover:text-brand-green-hover">
						{$_('panel.phases.addRow')}
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
