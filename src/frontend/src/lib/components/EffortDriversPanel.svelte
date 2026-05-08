<script lang="ts">
	type Driver = { description: string; factor: number; comment: string };

	let {
		effortDrivers,
		editable,
		onchange
	}: {
		effortDrivers: any[];
		editable: boolean;
		onchange: (drivers: any[]) => void;
	} = $props();

	let items = $state<Driver[]>(
		effortDrivers.map((d) => ({
			description: d.description ?? '',
			factor: d.factor ?? 0,
			comment: d.comment ?? ''
		}))
	);
	let open = $state(true);

	const totalFactor = $derived(items.reduce((s, d) => s + (d.factor ?? 0), 0));

	function notify() {
		onchange(
			items.map((i) => ({ description: i.description, factor: i.factor, comment: i.comment || null }))
		);
	}

	function addRow() {
		items.push({ description: '', factor: 0, comment: '' });
		notify();
	}

	function deleteRow(i: number) {
		items.splice(i, 1);
		notify();
	}

	function update(i: number, field: keyof Driver, raw: string) {
		(items[i] as any)[field] = field === 'factor' ? (raw === '' ? 0 : parseFloat(raw)) : raw;
		notify();
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>Effort Drivers</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		{#if items.length === 0 && !editable}
			<p class="p-4 text-sm text-gray-400 text-center">No effort drivers</p>
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
						<th class="py-2 px-3 text-left">Description</th>
						<th class="py-2 px-3 text-right w-28">Factor</th>
						<th class="py-2 px-3 text-left">Comment</th>
						{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each items as item, i}
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
										title="Delete"
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
							>Total factor:</td
						>
						<td class="py-2 px-3 text-right tabular-nums">{totalFactor.toFixed(2)}</td>
						<td class="py-2 px-3"></td>
						{#if editable}<td></td>{/if}
					</tr>
				</tfoot>
			</table>
			{#if editable}
				<div class="p-3 border-t bg-gray-50/40">
					<button onclick={addRow} class="text-sm text-brand-green hover:text-[#007a45]">
						+ Add driver
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
