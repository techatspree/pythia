<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import { _ } from 'svelte-i18n';

	// Shown when a drop would close a dependency loop, INSTEAD of committing it
	// (task-172). One action only: there is deliberately no "create anyway",
	// because a cyclic graph yields no schedule at all, so the override would
	// buy the user nothing.
	//
	// `items` are DISPLAY NAMES, already resolved by the caller. This dialog
	// does no lookups of its own — it never sees a logicalId, the labels map or
	// the schedule.
	let {
		items,
		oncancel
	}: {
		items: string[];
		oncancel: () => void;
	} = $props();

	// Keyboard access: focus the only action on open; ESC dismisses.
	let cancelButton = $state<HTMLButtonElement | null>(null);
	$effect(() => {
		cancelButton?.focus();
	});

	// Repeat the first name at the end so the loop reads as a loop:
	// "Alpha → Beta → Alpha".
	const loop = $derived(items.length > 0 ? [...items, items[0]].join(' → ') : '');
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
	aria-label={$_('schedule.cycleRefused.ariaLabel')}
	data-testid="cycle-refused"
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
		<h2 class="text-lg font-semibold mb-4">{$_('schedule.cycleRefused.title')}</h2>

		<p class="text-sm text-gray-600 mb-4">{$_('schedule.cycleRefused.body')}</p>

		<p class="text-sm font-medium mb-4 break-words" data-testid="cycle-refused-loop">{loop}</p>

		<div class="flex justify-end">
			<Button
				variant="secondary"
				bind:element={cancelButton}
				data-testid="cycle-refused-cancel"
				onclick={oncancel}>{$_('schedule.cycleRefused.cancel')}</Button
			>
		</div>
	</div>
</div>
