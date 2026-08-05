<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { EstimationDefaults } from '$lib/domain/domain.mjs';

	// The three calculation inputs are TYPED FIELDS, not user-named rows
	// (task-138). This panel used to let the user edit the parameter NAME while
	// the domain looked values up by the English strings
	// "dailyRate"/"stdDevFactor"/"salesSurcharge" — so a rename made the lookup
	// miss and the estimate silently fell back to the defaults. Fixed inputs
	// with i18n labels remove that failure mode and make the panel translatable.
	// The defaults come from the domain (EstimationDefaults), not a local copy —
	// the backend entities seed their columns from the same object, so "use
	// defaults" here can never drift from what an untouched draft actually holds.
	const defaults = EstimationDefaults.getInstance();
	const DEFAULT_DAILY_RATE = defaults.DAILY_RATE;
	const DEFAULT_STD_DEV_FACTOR = defaults.STD_DEV_FACTOR;
	const DEFAULT_SALES_SURCHARGE = defaults.SALES_SURCHARGE;

	let {
		dailyRate = $bindable<number>(DEFAULT_DAILY_RATE),
		stdDevFactor = $bindable<number>(DEFAULT_STD_DEV_FACTOR),
		salesSurcharge = $bindable<number>(DEFAULT_SALES_SURCHARGE),
		editable
	}: {
		dailyRate?: number;
		stdDevFactor?: number;
		salesSurcharge?: number;
		editable: boolean;
	} = $props();

	let open = $state(false);

	function useDefaults() {
		dailyRate = DEFAULT_DAILY_RATE;
		stdDevFactor = DEFAULT_STD_DEV_FACTOR;
		salesSurcharge = DEFAULT_SALES_SURCHARGE;
	}

	function num(raw: string, fallback: number): number {
		const parsed = parseFloat(raw);
		return raw === '' || Number.isNaN(parsed) ? fallback : parsed;
	}

	const rows = $derived([
		{
			key: 'dailyRate',
			id: 'param-daily-rate',
			label: $_('panel.parameters.dailyRate'),
			step: '1',
			value: dailyRate,
			set: (v: number) => (dailyRate = v),
			fallback: DEFAULT_DAILY_RATE
		},
		{
			key: 'stdDevFactor',
			id: 'param-std-dev-factor',
			label: $_('panel.parameters.stdDevFactor'),
			step: '0.1',
			value: stdDevFactor,
			set: (v: number) => (stdDevFactor = v),
			fallback: DEFAULT_STD_DEV_FACTOR
		},
		{
			key: 'salesSurcharge',
			id: 'param-sales-surcharge',
			label: $_('panel.parameters.salesSurcharge'),
			step: '0.01',
			value: salesSurcharge,
			set: (v: number) => (salesSurcharge = v),
			fallback: DEFAULT_SALES_SURCHARGE
		}
	]);
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>{$_('panel.parameters.title')}</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		<div class="p-4">
			<div class="grid grid-cols-[1fr_10rem] gap-x-4 gap-y-2 items-center max-w-xl">
				{#each rows as row (row.key)}
					<label class="text-sm text-gray-700" for={row.id}>{row.label}</label>
					{#if editable}
						<input
							id={row.id}
							type="number"
							step={row.step}
							min="0"
							data-testid="param-{row.key}"
							class="w-full text-right border rounded px-2 py-1 focus:outline-none focus:ring-1 focus:ring-brand-green/40"
							value={row.value}
							oninput={(e) => row.set(num(e.currentTarget.value, row.fallback))}
						/>
					{:else}
						<span class="text-right tabular-nums" data-testid="param-{row.key}">{row.value}</span>
					{/if}
				{/each}
			</div>

			{#if editable}
				<button
					type="button"
					onclick={useDefaults}
					class="mt-4 text-sm text-brand-green hover:text-[#007a45]"
				>
					{$_('panel.parameters.useDefaults')}
				</button>
			{/if}
		</div>
	{/if}
</div>
