<script lang="ts">
	import { dndzone, type DndEvent } from 'svelte-dnd-action';
	import { flip } from 'svelte/animate';

	type Bucket = { id: string; position: number; label: string };

	// Owns nothing: the route holds `currentBuckets` and binds it here. Mutations
	// (add/rename/delete/reorder) propagate back via $bindable (task-104), and the
	// autosave $effect persists them through the DraftUpdateDto.buckets field.
	let {
		buckets = $bindable<Bucket[]>([]),
		editable = false
	}: {
		buckets: Bucket[];
		editable?: boolean;
	} = $props();

	const flipDurationMs = 180;

	// Positions are the persisted order; keep them contiguous after every edit.
	function reindex(list: Bucket[]): Bucket[] {
		return list.map((b, i) => ({ ...b, position: i }));
	}

	function addBucket() {
		buckets = reindex([...buckets, { id: crypto.randomUUID(), position: buckets.length, label: 'Neu' }]);
	}

	function removeBucket(id: string) {
		buckets = reindex(buckets.filter((b) => b.id !== id));
	}

	function onConsider(e: CustomEvent<DndEvent<Bucket>>) {
		buckets = e.detail.items;
	}
	function onFinalize(e: CustomEvent<DndEvent<Bucket>>) {
		buckets = reindex(e.detail.items);
	}
</script>

<div class="mb-4 border rounded-lg p-3 bg-gray-50/40">
	<div class="flex items-center justify-between mb-2">
		<h3 class="text-sm font-semibold text-gray-700">Buckets</h3>
		{#if editable}
			<button
				type="button"
				onclick={addBucket}
				class="text-xs text-brand-green hover:text-[#007a45]"
				title="Bucket hinzufügen">+ Bucket</button
			>
		{/if}
	</div>

	<div
		class="flex flex-wrap gap-2 min-h-[2.25rem]"
		use:dndzone={{ items: buckets, flipDurationMs, dragDisabled: !editable }}
		onconsider={onConsider}
		onfinalize={onFinalize}
	>
		{#each buckets as bucket (bucket.id)}
			<div
				animate:flip={{ duration: flipDurationMs }}
				class="flex items-center gap-1 border rounded px-2 py-1 bg-white"
			>
				{#if editable}
					<span class="cursor-grab text-gray-300 select-none" aria-hidden="true" title="Ziehen zum Sortieren">⠿</span>
					<input
						type="text"
						class="bg-transparent text-sm w-24 focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1"
						value={bucket.label}
						aria-label="Bucket-Name"
						oninput={(e) => (bucket.label = e.currentTarget.value)}
					/>
					<button
						type="button"
						onclick={() => removeBucket(bucket.id)}
						class="text-gray-300 hover:text-red-500 leading-none"
						aria-label="Bucket löschen"
						title="Löschen">✕</button
					>
				{:else}
					<span class="text-sm text-gray-700">{bucket.label}</span>
				{/if}
			</div>
		{/each}
	</div>

	{#if buckets.length === 0}
		<p class="text-xs text-gray-400 mt-1">Keine Buckets — füge welche hinzu, um Items zuzuordnen.</p>
	{/if}
</div>
