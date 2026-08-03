<script lang="ts">
	import { _ } from 'svelte-i18n';

	// Confirmation before a DESTRUCTIVE replace: importing a Merlin project when a
	// draft already exists deletes that draft. Focus Cancel (not the destructive
	// action) on open, and ESC cancels — an accidental Enter must not lose data.
	let { onconfirm, oncancel }: { onconfirm: () => void; oncancel: () => void } = $props();

	let cancelButton = $state<HTMLButtonElement | null>(null);
	$effect(() => {
		cancelButton?.focus();
	});
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
	aria-label={$_('estimation.replaceDraftTitle')}
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
		<h2 class="text-lg font-semibold mb-4">{$_('estimation.replaceDraftTitle')}</h2>
		<p class="text-sm text-gray-600 mb-4">{$_('estimation.replaceDraftBody')}</p>
		<div class="flex justify-end gap-2">
			<button
				bind:this={cancelButton}
				type="button"
				onclick={oncancel}
				class="px-4 py-2 text-sm border rounded hover:bg-gray-50">{$_('estimation.replaceDraftCancel')}</button
			>
			<button
				type="button"
				onclick={onconfirm}
				class="px-4 py-2 text-sm bg-red-600 text-white rounded hover:bg-red-700"
				>{$_('estimation.importMerlinReplace')}</button
			>
		</div>
	</div>
</div>
