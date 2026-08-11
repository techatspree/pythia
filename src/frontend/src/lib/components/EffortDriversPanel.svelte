<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);

	type Driver = { description: string; factor: number; comment: string };

	let {
		effortDrivers = $bindable<Driver[]>([]),
		editable
	}: {
		effortDrivers?: Driver[];
		editable: boolean;
	} = $props();

	let open = $state(false);

	const totalFactor = $derived(effortDrivers.reduce((s, d) => s + (d.factor ?? 0), 0));

	function addRow() {
		effortDrivers.push({ description: '', factor: 0, comment: '' });
	}

	function deleteRow(i: number) {
		effortDrivers.splice(i, 1);
	}

	function update(i: number, field: keyof Driver, raw: string) {
		(effortDrivers[i] as any)[field] = field === 'factor' ? (raw === '' ? 0 : parseFloat(raw)) : raw;
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>{$_('panel.effortDrivers.title')}</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		{#if effortDrivers.length === 0 && !editable}
			<p class="p-4 text-sm text-gray-400 text-center">{$_('panel.effortDrivers.emptyReadonly')}</p>
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
						<th class="py-2 px-3 text-left">{$_('panel.effortDrivers.colDescription')}</th>
						<th class="py-2 px-3 text-right w-28">{$_('panel.effortDrivers.colFactor')}</th>
						<th class="py-2 px-3 text-left">{$_('panel.effortDrivers.colComment')}</th>
						{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each effortDrivers as item, i (i)}
						<tr class="border-b hover:bg-gray-50">
							<td class="py-1 px-3">
								{#if editable}
									<input
										type="text"
										class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.description}
										oninput={(e) => update(i, 'description', e.currentTarget.value)}
									/>
								{:else}
									{item.description}
								{/if}
							</td>
							<td class="py-1 px-3 text-right">
								{#if editable}
									<input
										type="number"
										step="0.01"
										class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.factor}
										oninput={(e) => update(i, 'factor', e.currentTarget.value)}
									/>
								{:else}
									<span class="tabular-nums">{item.factor}</span>
								{/if}
							</td>
							<td class="py-1 px-3">
								{#if editable}
									<input
										type="text"
										class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.comment}
										oninput={(e) => update(i, 'comment', e.currentTarget.value)}
									/>
								{:else}
									<span class="text-gray-500">{item.comment}</span>
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
					{/each}
				</tbody>
				<tfoot>
					<tr class="border-t bg-gray-50 font-medium text-sm">
						<td class="py-2 px-3 text-right text-gray-500 text-xs uppercase tracking-wide"
							>{$_('panel.effortDrivers.totalFactor')}</td
						>
						<td class="py-2 px-3 text-right tabular-nums">{num(totalFactor, 2)}</td>
						<td class="py-2 px-3"></td>
						{#if editable}<td></td>{/if}
					</tr>
				</tfoot>
			</table>
			{#if editable}
				<div class="p-3 border-t bg-gray-50/40">
					<button onclick={addRow} class="text-sm text-brand-green hover:text-brand-green-hover">
						{$_('panel.effortDrivers.addRow')}
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
