<script lang="ts">
	import { dndzone, type DndEvent } from 'svelte-dnd-action';
	import { flip } from 'svelte/animate';
	import { _ } from 'svelte-i18n';

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

	// TreeTable globally renames svelte-dnd-action's id key to `__treeTableId`
	// (`overrideItemIdKeyNameBeforeInitialisingDndZones`), and that override is
	// process-wide — it applies to THIS zone too, because the editor renders both
	// components on one page. Feeding raw buckets here therefore throws
	// "missing '__treeTableId' property for item" during zone init, which killed
	// drag-and-drop for the whole page. Wrap each bucket instead, keeping the
	// object identity so the label editor still mutates the real bucket.
	type Wrapped = { __treeTableId: string; bucket: Bucket };

	const wrapped = $derived(buckets.map((b) => ({ __treeTableId: b.id, bucket: b })));

	function unwrap(items: Wrapped[]): Bucket[] {
		return items.filter((w) => w.bucket != null).map((w) => w.bucket);
	}

	// Positions are the persisted order; keep them contiguous after every edit.
	function reindex(list: Bucket[]): Bucket[] {
		return list.map((b, i) => ({ ...b, position: i }));
	}

	function addBucket() {
		buckets = reindex([
			...buckets,
			{ id: crypto.randomUUID(), position: buckets.length, label: $_('bucket.defaultLabel') }
		]);
	}

	function removeBucket(id: string) {
		buckets = reindex(buckets.filter((b) => b.id !== id));
	}

	function onConsider(e: CustomEvent<DndEvent<Wrapped>>) {
		buckets = unwrap(e.detail.items);
	}
	function onFinalize(e: CustomEvent<DndEvent<Wrapped>>) {
		buckets = reindex(unwrap(e.detail.items));
	}
</script>

<div class="mb-4 border rounded-lg p-3 bg-gray-50/40">
	<div class="flex items-center justify-between mb-2">
		<h3 class="text-sm font-semibold text-gray-700">{$_('bucket.panelTitle')}</h3>
		{#if editable}
			<button
				type="button"
				onclick={addBucket}
				class="text-xs text-brand-green hover:text-[#007a45]"
				title={$_('bucket.addTitle')}>{$_('bucket.add')}</button
			>
		{/if}
	</div>

	<div
		class="flex flex-wrap gap-2 min-h-[2.25rem]"
		use:dndzone={{ items: wrapped, flipDurationMs, dragDisabled: !editable }}
		onconsider={onConsider}
		onfinalize={onFinalize}
	>
		{#each wrapped as w (w.__treeTableId)}
			{@const bucket = w.bucket}
			<div
				animate:flip={{ duration: flipDurationMs }}
				class="flex items-center gap-1 border rounded px-2 py-1 bg-white"
			>
				{#if editable}
					<span class="cursor-grab text-gray-300 select-none" aria-hidden="true" title={$_('bucket.dragTitle')}>⠿</span>
					<input
						type="text"
						class="bg-transparent text-sm w-24 focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1"
						value={bucket.label}
						aria-label={$_('bucket.nameAria')}
						oninput={(e) => (bucket.label = e.currentTarget.value)}
					/>
					<button
						type="button"
						onclick={() => removeBucket(bucket.id)}
						class="text-gray-300 hover:text-red-500 leading-none"
						aria-label={$_('bucket.deleteAria')}
						title={$_('bucket.deleteTitle')}>✕</button
					>
				{:else}
					<span class="text-sm text-gray-700">{bucket.label}</span>
				{/if}
			</div>
		{/each}
	</div>

	{#if buckets.length === 0}
		<p class="text-xs text-gray-400 mt-1">{$_('bucket.empty')}</p>
	{/if}
</div>
