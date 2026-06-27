<script lang="ts">
	import type { ApiAdditionalCost, ApiPhase } from '$lib/api/types.js';

	let {
		costs,
		phases,
		editable,
		onchange
	}: {
		costs: ApiAdditionalCost[];
		phases: ApiPhase[];
		editable: boolean;
		onchange: (costs: ApiAdditionalCost[]) => void;
	} = $props();

	let items = $state<ApiAdditionalCost[]>(
		costs.map((c) => ({
			id: c.id ?? null,
			description: c.description ?? '',
			amount: c.amount ?? 0,
			type: c.type,
			amountPerWeek: c.amountPerWeek ?? null,
			phaseAbbreviation: c.phaseAbbreviation ?? null
		}))
	);
	let open = $state(false);

	const oneTimeCount = $derived(items.filter((c) => c.type === 'ONE_TIME').length);
	const recurringCount = $derived(items.filter((c) => c.type === 'RECURRING').length);

	function notify() {
		onchange(
			items.map((c) => ({
				id: c.id,
				description: c.description,
				amount: c.amount,
				type: c.type,
				amountPerWeek: c.amountPerWeek,
				phaseAbbreviation: c.phaseAbbreviation
			}))
		);
	}

	function addOneTime() {
		items.push({
			id: null,
			description: '',
			amount: 0,
			type: 'ONE_TIME',
			amountPerWeek: null,
			phaseAbbreviation: null
		});
		notify();
	}

	function addRecurring() {
		items.push({
			id: null,
			description: '',
			amount: 0,
			type: 'RECURRING',
			amountPerWeek: 0,
			phaseAbbreviation: null
		});
		notify();
	}

	function deleteRow(i: number) {
		items.splice(i, 1);
		notify();
	}

	function updateDescription(i: number, raw: string) {
		items[i].description = raw;
		notify();
	}

	function updatePhase(i: number, raw: string) {
		items[i].phaseAbbreviation = raw === '' ? null : raw;
		notify();
	}

	function updateAmount(i: number, raw: string) {
		items[i].amount = raw === '' ? 0 : parseFloat(raw);
		notify();
	}

	function updateAmountPerWeek(i: number, raw: string) {
		items[i].amountPerWeek = raw === '' ? null : parseFloat(raw);
		notify();
	}

	function totalRecurring(cost: ApiAdditionalCost): string {
		if (cost.amountPerWeek == null || cost.phaseAbbreviation == null) return '—';
		const phase = phases.find((p) => p.abbreviation === cost.phaseAbbreviation);
		if (phase == null || phase.durationWeeks == null) return '—';
		return (cost.amountPerWeek * phase.durationWeeks).toFixed(2);
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>Additional Costs</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		<div class="border-b">
			<div class="px-4 py-2 text-xs text-gray-500 uppercase tracking-wide bg-gray-50/40">
				Einmalige Kosten
			</div>
			{#if oneTimeCount === 0}
				{#if editable}
					<div class="p-4 text-center">
						<p class="text-sm text-gray-400 mb-3">No one-time costs</p>
						<button
							onclick={addOneTime}
							class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
						>
							Add one-time cost
						</button>
					</div>
				{:else}
					<p class="p-4 text-sm text-gray-400 text-center">No one-time costs</p>
				{/if}
			{:else}
				<table class="w-full text-sm border-collapse">
					<thead>
						<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
							<th class="py-2 px-3 text-left">Description</th>
							<th class="py-2 px-3 text-right w-32">Amount (€)</th>
							<th class="py-2 px-3 text-left w-32">Phase</th>
							{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
						</tr>
					</thead>
					<tbody>
						{#each items as cost, i (i)}
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
											<span class="tabular-nums">{cost.amount.toFixed(2)}</span>
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
												title="Delete"
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
							+ Add one-time cost
						</button>
					</div>
				{/if}
			{/if}
		</div>

		<div>
			<div class="px-4 py-2 text-xs text-gray-500 uppercase tracking-wide bg-gray-50/40">
				Laufende Kosten
			</div>
			{#if recurringCount === 0}
				{#if editable}
					<div class="p-4 text-center">
						<p class="text-sm text-gray-400 mb-3">No recurring costs</p>
						<button
							onclick={addRecurring}
							class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
						>
							Add recurring cost
						</button>
					</div>
				{:else}
					<p class="p-4 text-sm text-gray-400 text-center">No recurring costs</p>
				{/if}
			{:else}
				<table class="w-full text-sm border-collapse">
					<thead>
						<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
							<th class="py-2 px-3 text-left">Description</th>
							<th class="py-2 px-3 text-right w-32">€/Week</th>
							<th class="py-2 px-3 text-left w-32">Phase</th>
							<th class="py-2 px-3 text-right w-32">Total (€)</th>
							{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
						</tr>
					</thead>
					<tbody>
						{#each items as cost, i (i)}
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
											<span class="tabular-nums">{cost.amountPerWeek?.toFixed(2) ?? ''}</span>
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
												title="Delete"
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
							+ Add recurring cost
						</button>
					</div>
				{/if}
			{/if}
		</div>
	{/if}
</div>
