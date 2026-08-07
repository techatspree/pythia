<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import { ZERO_TOTALS, type EstimationTotalsView } from '$lib/adapter';

	// The summed figures of the WHOLE estimation, shown at the top of the version
	// editor so a total exists for every method — the bucket+sampled editor
	// aggregates only per bucket, and had no overall number at all. Every value
	// here is computed by the domain reducer (EstimationVersion.totals()); this
	// component only formats.
	let { totals = ZERO_TOTALS }: { totals?: EstimationTotalsView } = $props();

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);

	const effortRows = $derived([
		{ key: 'meanPT', label: $_('summary.meanPT'), value: num(totals.meanPT, 2) },
		{
			key: 'riskSurchargePT',
			label: $_('summary.riskSurchargePT'),
			value: num(totals.riskSurchargePT, 2)
		},
		{
			key: 'driverSurchargePT',
			label: $_('summary.driverSurchargePT'),
			value: num(totals.driverSurchargePT, 2)
		}
	]);

	const moneyRows = $derived([
		{
			key: 'developmentCost',
			label: $_('summary.developmentCost'),
			value: num(totals.developmentCost, 0)
		},
		{
			key: 'additionalOneTime',
			label: $_('summary.additionalOneTime'),
			value: num(totals.additionalOneTime, 0)
		},
		{
			key: 'additionalRecurring',
			label: $_('summary.additionalRecurring'),
			value: num(totals.additionalRecurring, 0)
		},
		{
			key: 'salesSurcharge',
			label: $_('summary.salesSurcharge'),
			value: num(totals.salesSurchargeAmount, 0)
		}
	]);
</script>

<div
	class="border rounded-lg overflow-hidden mb-4 bg-brand-green/5"
	data-testid="version-summary"
>
	<div
		class="px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide flex items-center justify-between"
	>
		<span>{$_('summary.title')}</span>
		<span class="normal-case font-normal text-gray-500">
			{$_('summary.leafCount', { values: { count: totals.leafCount } })}
		</span>
	</div>

	<div class="p-4 grid gap-6 md:grid-cols-2">
		<div>
			<dl class="grid grid-cols-[1fr_auto] gap-x-6 gap-y-1 text-sm">
				{#each effortRows as row (row.key)}
					<dt class="text-gray-600">{row.label}</dt>
					<dd class="text-right tabular-nums" data-testid="version-summary.{row.key}">
						{row.value}
					</dd>
				{/each}
			</dl>
			<div
				class="mt-2 pt-2 border-t flex items-baseline justify-between gap-6 font-semibold"
			>
				<span class="text-gray-700">{$_('summary.offerPT')}</span>
				<span
					class="text-lg text-brand-green tabular-nums"
					data-testid="version-summary.offerPT">{num(totals.offerPT, 2)}</span
				>
			</div>
		</div>

		<div>
			<dl class="grid grid-cols-[1fr_auto] gap-x-6 gap-y-1 text-sm">
				{#each moneyRows as row (row.key)}
					<dt class="text-gray-600">{row.label}</dt>
					<dd class="text-right tabular-nums" data-testid="version-summary.{row.key}">
						{row.value}
					</dd>
				{/each}
			</dl>
			<div
				class="mt-2 pt-2 border-t flex items-baseline justify-between gap-6 font-semibold"
			>
				<span class="text-gray-700">{$_('summary.totalOfferPrice')}</span>
				<span
					class="text-lg text-brand-green tabular-nums"
					data-testid="version-summary.totalOfferPrice">{num(totals.totalOfferPrice, 0)}</span
				>
			</div>
		</div>
	</div>

	{#if totals.recurringWithoutPhase > 0}
		<p class="px-4 pb-3 -mt-1 text-sm text-amber-700" data-testid="version-summary.warning">
			{$_('summary.recurringWithoutPhase', { values: { count: totals.recurringWithoutPhase } })}
		</p>
	{/if}
</div>
