<script lang="ts">
	import { formatMethodLabel } from '$lib/methods/labels';
	import { log } from '$lib/log';
	import type { components } from '$lib/api/schema';

	type EstimationMethod = components['schemas']['EstimationMethod'];

	let {
		method,
		description,
		open = $bindable(false),
		onclose
	}: {
		method: EstimationMethod;
		description: string;
		open: boolean;
		onclose?: () => void;
	} = $props();

	let container = $state<HTMLDivElement | null>(null);

	function close() {
		open = false;
		onclose?.();
	}

	$effect(() => {
		if (!open) return;
		log.debug('MethodDetailPopover: opened for method', method);

		function onKey(e: KeyboardEvent) {
			if (e.key === 'Escape') close();
		}
		function onClick(e: MouseEvent) {
			if (container && !container.contains(e.target as Node)) close();
		}

		// Defer the click listener by a microtask so the very click that opened
		// the popover does not immediately close it.
		const clickHandle = setTimeout(() => {
			window.addEventListener('click', onClick);
		}, 0);
		window.addEventListener('keydown', onKey);
		return () => {
			clearTimeout(clickHandle);
			window.removeEventListener('click', onClick);
			window.removeEventListener('keydown', onKey);
		};
	});
</script>

{#if open}
	<div
		bind:this={container}
		data-testid="estimation-detail.method-detail-popover"
		role="dialog"
		aria-label="Method description"
		class="absolute z-50 mt-2 bg-white rounded-lg shadow-xl border p-4 max-w-sm text-sm text-gray-700"
	>
		<div class="font-semibold text-brand-green mb-2">{formatMethodLabel(method)}</div>
		<p>{description}</p>
	</div>
{/if}
