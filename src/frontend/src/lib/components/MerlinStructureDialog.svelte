<script lang="ts" module>
	// Body of the 409 the Merlin export answers with when the uploaded document's
	// WBS no longer matches the estimation tree (task-133).
	export type MerlinStructureDiff = {
		missingInMerlin: string[];
		missingInEstimation: string[];
		reordered: string[];
		inSync: boolean;
	};
</script>

<script lang="ts">
	import { _ } from 'svelte-i18n';

	// Confirmation before a DESTRUCTIVE overwrite: continuing rewrites the WBS in
	// the Merlin copy to match this application, which deletes activities that no
	// longer exist here. Focus Cancel (not the destructive action) on open, and
	// ESC cancels — an accidental Enter must not restructure a project plan.
	let {
		diff,
		onconfirm,
		oncancel
	}: { diff: MerlinStructureDiff; onconfirm: () => void; oncancel: () => void } = $props();

	let cancelButton = $state<HTMLButtonElement | null>(null);
	$effect(() => {
		cancelButton?.focus();
	});

	const groups = $derived([
		{ label: $_('merlin.diffAdded'), paths: diff.missingInMerlin },
		{ label: $_('merlin.diffRemoved'), paths: diff.missingInEstimation },
		{ label: $_('merlin.diffReordered'), paths: diff.reordered }
	]);
</script>

<svelte:window
	onkeydown={(e) => {
		if (e.key === 'Escape') oncancel();
	}}
/>

<div
	class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
	role="dialog"
	aria-modal="true"
	aria-label={$_('merlin.diffTitle')}
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-lg">
		<h2 class="text-lg font-semibold mb-4">{$_('merlin.diffTitle')}</h2>
		<p class="text-sm text-gray-600 mb-4">{$_('merlin.diffBody')}</p>

		{#each groups as group (group.label)}
			{#if group.paths.length > 0}
				<div class="mb-3">
					<h3 class="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">
						{group.label} ({group.paths.length})
					</h3>
					<ul class="max-h-32 overflow-y-auto border rounded bg-gray-50/60 text-xs text-gray-700">
						{#each group.paths as path (path)}
							<li class="px-2 py-1 border-b last:border-b-0 break-words">{path}</li>
						{/each}
					</ul>
				</div>
			{/if}
		{/each}

		<div class="flex justify-end gap-2 mt-4">
			<button
				bind:this={cancelButton}
				type="button"
				onclick={oncancel}
				class="px-4 py-2 text-sm border rounded hover:bg-gray-50">{$_('merlin.cancel')}</button
			>
			<button
				type="button"
				onclick={onconfirm}
				class="px-4 py-2 text-sm bg-red-600 text-white rounded hover:bg-red-700"
				>{$_('merlin.overwrite')}</button
			>
		</div>
	</div>
</div>
