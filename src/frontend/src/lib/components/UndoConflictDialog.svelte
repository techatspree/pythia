<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatDate, DEFAULT_LOCALE } from '$lib/format';
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
		return formatDate(iso, $locale ?? DEFAULT_LOCALE, { dateStyle: 'short', timeStyle: 'short' });
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
	aria-label={$_('history.conflict.ariaLabel')}
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
		<h2 class="text-lg font-semibold mb-4">{$_('history.conflict.title')}</h2>

		<p class="text-sm text-gray-600 mb-4">
			<span class="font-medium">{conflict.blockingUserDisplayName}</span>{$_('history.conflict.body', {
				values: { kind: conflict.blockingKind, time: formatTime(conflict.blockingCreatedAt) }
			})}
		</p>

		<div class="flex justify-end gap-2">
			<button
				type="button"
				onclick={oncancel}
				class="px-4 py-2 text-sm border rounded hover:bg-gray-50">{$_('history.conflict.cancel')}</button
			>
			<button
				bind:this={reloadButton}
				type="button"
				onclick={onreload}
				class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
				>{$_('history.conflict.reload')}</button
			>
		</div>
	</div>
</div>
