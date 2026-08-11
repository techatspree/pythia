<script lang="ts">
	import type { Snippet } from 'svelte';

	// The SINGLE definition of the bordered card (task-149): the shell the four
	// editor panels and the session pages share, plus the brand-green header
	// strip that titles it.
	//
	// Three shapes, all real in this codebase:
	//   <Card title="…">          titled card  — strip + padded body
	//   <Card>                    untitled     — padded body only (status cards)
	//   <Card {header} padded={false}>         — caller owns strip AND body,
	//                                            for the collapsible editor
	//                                            panels whose strip is a toggle
	//                                            button and whose body is `{#if
	//                                            open}`-gated.
	let {
		title = undefined,
		header = undefined,
		padded = true,
		class: extraClass = '',
		children
	}: {
		title?: string;
		header?: Snippet;
		padded?: boolean;
		class?: string;
		children?: Snippet;
	} = $props();

	const shell = $derived(
		['border rounded-lg overflow-hidden', extraClass].filter(Boolean).join(' ')
	);
</script>

<div class={shell}>
	{#if header}
		{@render header()}
	{:else if title}
		<div
			class="px-4 py-2 bg-brand-green/10 text-brand-green text-xs font-semibold uppercase tracking-wide"
		>
			{title}
		</div>
	{/if}
	{#if padded}
		<div class="p-4">{@render children?.()}</div>
	{:else}
		{@render children?.()}
	{/if}
</div>
