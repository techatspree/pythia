<script lang="ts">
	type Param = { name: string; value: number; comment: string };

	const DEFAULTS: Param[] = [
		{ name: 'Tagessatz', value: 800, comment: '' },
		{ name: 'Standardabweichungsfaktor', value: 2.0, comment: '' },
		{ name: 'Vertriebszuschlag', value: 0.1, comment: '' }
	];

	let {
		parameters,
		editable,
		onchange
	}: {
		parameters: Param[];
		editable: boolean;
		onchange: (params: Param[]) => void;
	} = $props();

	let items = $state<Param[]>(
		parameters.map((p) => ({ name: p.name ?? '', value: p.value ?? 0, comment: p.comment ?? '' }))
	);
	let open = $state(false);

	function notify() {
		onchange(items.map((i) => ({ name: i.name, value: i.value, comment: i.comment })));
	}

	function addRow() {
		items.push({ name: '', value: 0, comment: '' });
		notify();
	}

	function deleteRow(i: number) {
		items.splice(i, 1);
		notify();
	}

	function useDefaults() {
		items = DEFAULTS.map((p) => ({ ...p }));
		notify();
	}

	function update(i: number, field: keyof Param, raw: string) {
		(items[i] as any)[field] = field === 'value' ? (raw === '' ? 0 : parseFloat(raw)) : raw;
		notify();
	}
</script>

<div class="border rounded-lg overflow-hidden mb-4">
	<button
		class="w-full flex items-center justify-between px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide hover:bg-brand-green/20"
		onclick={() => (open = !open)}
	>
		<span>Parameters</span>
		<span>{open ? '▼' : '▶'}</span>
	</button>

	{#if open}
		{#if items.length === 0}
			{#if editable}
				<div class="p-4 text-center">
					<p class="text-sm text-gray-400 mb-3">No parameters defined.</p>
					<button
						onclick={useDefaults}
						class="px-3 py-1.5 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>
						Use defaults
					</button>
				</div>
			{:else}
				<p class="p-4 text-sm text-gray-400 text-center">No parameters</p>
			{/if}
		{:else}
			<table class="w-full text-sm border-collapse">
				<thead>
					<tr class="border-b text-xs text-gray-500 uppercase tracking-wide">
						<th class="py-2 px-3 text-left">Name</th>
						<th class="py-2 px-3 text-right w-32">Value</th>
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
										value={item.name}
										oninput={(e) => update(i, 'name', e.currentTarget.value)}
									/>
								{:else}
									{item.name}
								{/if}
							</td>
							<td class="py-1 px-3 text-right">
								{#if editable}
									<input
										type="number"
										step="any"
										class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
										value={item.value}
										oninput={(e) => update(i, 'value', e.currentTarget.value)}
									/>
								{:else}
									<span class="tabular-nums">{item.value}</span>
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
			</table>
			{#if editable}
				<div class="p-3 border-t bg-gray-50/40">
					<button onclick={addRow} class="text-sm text-brand-green hover:text-[#007a45]">
						+ Add parameter
					</button>
				</div>
			{/if}
		{/if}
	{/if}
</div>
