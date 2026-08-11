<script lang="ts">
	import type { Snippet } from 'svelte';

	// The page shell AND the single definition of content width (task-154).
	//
	// Width used to be chosen per component — 7 different `max-w-*` values across
	// 17 sites — which is why the session setup page's Card (`max-w-3xl`) and the
	// block holding its item picker (`max-w-2xl`) disagreed about their right
	// edge, visible as a ragged column.
	//
	// **The container owns the column.** A page's children must NOT carry their
	// own `max-w-*`; if they do they can drift apart again, which is the defect
	// this component exists to make unrepresentable.
	let {
		width = 'full',
		class: extraClass = '',
		children
	}: {
		/** 'full' — full-bleed working surface (projects, estimations, editor,
		 *  session room). 'form' — a single readable column for form-shaped pages. */
		width?: 'full' | 'form';
		class?: string;
		children?: Snippet;
	} = $props();

	const outer = $derived(['p-6', extraClass].filter(Boolean).join(' '));
</script>

<div class={outer}>
	{#if width === 'form'}
		<div class="max-w-3xl">{@render children?.()}</div>
	{:else}
		{@render children?.()}
	{/if}
</div>
