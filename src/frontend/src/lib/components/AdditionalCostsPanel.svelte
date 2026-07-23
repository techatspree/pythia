<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import type { ApiAdditionalCost, ApiPhase } from '$lib/api/types.js';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);

	let {
		costs = $bindable<ApiAdditionalCost[]>([]),
		phases,
		editable
	}: {
		costs?: ApiAdditionalCost[];
		phases: ApiPhase[];
		editable: boolean;
	} = $props();

	let open = $state(false);

	const oneTimeCount = $derived(costs.filter((c) => c.type === 'ONE_TIME').length);
	const recurringCount = $derived(costs.filter((c) => c.type === 'RECURRING').length);

	function addOneTime() {
		costs.push({
			id: null,
			description: '',
			amount: 0,
			type: 'ONE_TIME',
			amountPerWeek: null,
			phaseAbbreviation: null
		});
	}

	function addRecurring() {
		costs.push({
			id: null,
			description: '',
			amount: 0,
			type: 'RECURRING',
			amountPerWeek: 0,
			phaseAbbreviation: null
		});
	}

	function deleteRow(i: number) {
		costs.splice(i, 1);
	}

	function updateDescription(i: number, raw: string) {
		costs[i].description = raw;
	}

	function updatePhase(i: number, raw: string) {
		costs[i].phaseAbbreviation = raw === '' ? null : raw;
	}

	function updateAmount(i: number, raw: string) {
		costs[i].amount = raw === '' ? 0 : parseFloat(raw);
	}

	function updateAmountPerWeek(i: number, raw: string) {
		costs[i].amountPerWeek = raw === '' ? null : parseFloat(raw);
	}

	function totalRecurring(cost: ApiAdditionalCost): string {
		if (cost.amountPerWeek == null || cost.phaseAbbreviation == null) return '—';
		const phase = phases.find((p) => p.abbreviation === cost.phaseAbbreviation);
		if (phase == null || phase.durationWeeks == null) return '—';
		return num(cost.amountPerWeek * phase.durationWeeks, 2);
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>{$_('panel.additionalCosts.title')}</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		<div class="border-b">
			<div class="px-4 py-2 text-xs text-gray-500 uppercase tracking-wide bg-gray-50/40">
				{$_('panel.additionalCosts.oneTimeSection')}
			</div>
			{#if oneTimeCount === 0}
				{#if editable}
					<div class="p-4 text-center">
						<p class="text-sm text-gray-400 mb-3">{$_('panel.additionalCosts.oneTimeEmpty')}</p>
						<button
							onclick={addOneTime}
							class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
						>
							{$_('panel.additionalCosts.oneTimeAdd')}
						</button>
					</div>
				{:else}
					<p class="p-4 text-sm text-gray-400 text-center">{$_('panel.additionalCosts.oneTimeEmpty')}</p>
				{/if}
			{:else}
				<table class="w-full text-sm border-collapse">
					<thead>
						<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
							<th class="py-2 px-3 text-left">{$_('panel.additionalCosts.colDescription')}</th>
							<th class="py-2 px-3 text-right w-32">{$_('panel.additionalCosts.colAmount')}</th>
							<th class="py-2 px-3 text-left w-32">{$_('panel.additionalCosts.colPhase')}</th>
							{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
						</tr>
					</thead>
					<tbody>
						{#each costs as cost, i (i)}
							{#if cost.type === 'ONE_TIME'}
								<tr class="border-b hover:bg-gray-50">
									<td class="py-1 px-3">
										{#if editable}
											<input
												type="text"
												class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.description}
												oninput={(e) => updateDescription(i, e.currentTarget.value)}
											/>
										{:else}
											{cost.description}
										{/if}
									</td>
									<td class="py-1 px-3 text-right">
										{#if editable}
											<input
												type="number"
												step="0.01"
												class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.amount}
												oninput={(e) => updateAmount(i, e.currentTarget.value)}
											/>
										{:else}
											<span class="tabular-nums">{num(cost.amount, 2)}</span>
										{/if}
									</td>
									<td class="py-1 px-3">
										{#if editable}
											<select
												class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.phaseAbbreviation ?? ''}
												onchange={(e) => updatePhase(i, e.currentTarget.value)}
											>
												<option value="">—</option>
												{#each phases as p (p.abbreviation)}
													<option value={p.abbreviation}>{p.abbreviation}</option>
												{/each}
											</select>
										{:else}
											<span class="text-gray-600">{cost.phaseAbbreviation ?? ''}</span>
										{/if}
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
							{/if}
						{/each}
					</tbody>
				</table>
				{#if editable}
					<div class="p-3 border-t bg-gray-50/40">
						<button onclick={addOneTime} class="text-sm text-brand-green hover:text-[#007a45]">
							{$_('panel.additionalCosts.oneTimeAddRow')}
						</button>
					</div>
				{/if}
			{/if}
		</div>

		<div>
			<div class="px-4 py-2 text-xs text-gray-500 uppercase tracking-wide bg-gray-50/40">
				{$_('panel.additionalCosts.recurringSection')}
			</div>
			{#if recurringCount === 0}
				{#if editable}
					<div class="p-4 text-center">
						<p class="text-sm text-gray-400 mb-3">{$_('panel.additionalCosts.recurringEmpty')}</p>
						<button
							onclick={addRecurring}
							class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
						>
							{$_('panel.additionalCosts.recurringAdd')}
						</button>
					</div>
				{:else}
					<p class="p-4 text-sm text-gray-400 text-center">{$_('panel.additionalCosts.recurringEmpty')}</p>
				{/if}
			{:else}
				<table class="w-full text-sm border-collapse">
					<thead>
						<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
							<th class="py-2 px-3 text-left">{$_('panel.additionalCosts.colDescription')}</th>
							<th class="py-2 px-3 text-right w-32">{$_('panel.additionalCosts.colAmountPerWeek')}</th>
							<th class="py-2 px-3 text-left w-32">{$_('panel.additionalCosts.colPhase')}</th>
							<th class="py-2 px-3 text-right w-32">{$_('panel.additionalCosts.colTotal')}</th>
							{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
						</tr>
					</thead>
					<tbody>
						{#each costs as cost, i (i)}
							{#if cost.type === 'RECURRING'}
								<tr class="border-b hover:bg-gray-50">
									<td class="py-1 px-3">
										{#if editable}
											<input
												type="text"
												class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.description}
												oninput={(e) => updateDescription(i, e.currentTarget.value)}
											/>
										{:else}
											{cost.description}
										{/if}
									</td>
									<td class="py-1 px-3 text-right">
										{#if editable}
											<input
												type="number"
												step="0.01"
												class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.amountPerWeek ?? ''}
												oninput={(e) => updateAmountPerWeek(i, e.currentTarget.value)}
											/>
										{:else}
											<span class="tabular-nums">{cost.amountPerWeek != null ? num(cost.amountPerWeek, 2) : ''}</span>
										{/if}
									</td>
									<td class="py-1 px-3">
										{#if editable}
											<select
												class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
												value={cost.phaseAbbreviation ?? ''}
												onchange={(e) => updatePhase(i, e.currentTarget.value)}
											>
												<option value="">—</option>
												{#each phases as p (p.abbreviation)}
													<option value={p.abbreviation}>{p.abbreviation}</option>
												{/each}
											</select>
										{:else}
											<span class="text-gray-600">{cost.phaseAbbreviation ?? ''}</span>
										{/if}
									</td>
									<td class="py-1 px-3 text-right text-gray-600 tabular-nums">
										{totalRecurring(cost)}
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
							{/if}
						{/each}
					</tbody>
				</table>
				{#if editable}
					<div class="p-3 border-t bg-gray-50/40">
						<button onclick={addRecurring} class="text-sm text-brand-green hover:text-[#007a45]">
							{$_('panel.additionalCosts.recurringAddRow')}
						</button>
					</div>
				{/if}
			{/if}
		</div>
	{/if}
</div>
