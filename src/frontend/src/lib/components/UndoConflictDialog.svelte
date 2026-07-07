<script lang="ts">
	import type { components } from '$lib/api/schema';

	type ConflictDetailsDto = components['schemas']['ConflictDetailsDto'];

	let {
		conflict,
		onreload,
		oncancel
	}: {
		conflict: ConflictDetailsDto;
		onreload: () => void;
		oncancel: () => void;
	} = $props();

	// Keyboard access: focus the recommended action on open; ESC cancels.
	let reloadButton = $state<HTMLButtonElement | null>(null);
	$effect(() => {
		reloadButton?.focus();
	});

	function formatTime(iso: string | null | undefined): string {
		if (!iso) return '';
		const d = new Date(iso);
		return Number.isNaN(d.getTime()) ? '' : d.toLocaleString('de-DE');
	}
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
	aria-label="Konflikt beim Rückgängigmachen"
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
		<h2 class="text-lg font-semibold mb-4">Änderung kann nicht rückgängig gemacht werden</h2>

		<p class="text-sm text-gray-600 mb-4">
			<span class="font-medium">{conflict.blockingUserDisplayName}</span> hat zwischenzeitlich eine
			neuere Änderung ({conflict.blockingKind}, {formatTime(conflict.blockingCreatedAt)})
			vorgenommen. Ihr Rückgängigmachen würde diese Arbeit überschreiben und ist daher nicht möglich.
		</p>

		<div class="flex justify-end gap-2">
			<button
				type="button"
				onclick={oncancel}
				class="px-4 py-2 text-sm border rounded hover:bg-gray-50">Abbrechen</button
			>
			<button
				bind:this={reloadButton}
				type="button"
				onclick={onreload}
				class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
				>Aktuellen Stand neu laden</button
			>
		</div>
	</div>
</div>
