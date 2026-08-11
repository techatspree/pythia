<script lang="ts">
	import type { Snippet } from 'svelte';

	// Label + control + optional hint (task-154; the primitive task-149 deferred).
	// The label markup was repeated at 14 sites with no shared definition.
	//
	// The caller passes the control as children AND gives it the matching `id`:
	// Svelte cannot inject an attribute into an arbitrary child, so pretending to
	// wire the association here would silently produce an unlabelled control.
	let {
		label,
		id,
		hint = undefined,
		class: extraClass = 'mb-4',
		children
	}: {
		label: string;
		/** Must match the `id` of the control rendered as children. */
		id: string;
		hint?: string;
		class?: string;
		children?: Snippet;
	} = $props();
</script>

<div class={extraClass}>
	<label class="block text-sm font-medium mb-1" for={id}>{label}</label>
	{@render children?.()}
	{#if hint}
		<p class="text-xs text-gray-500 mt-1">{hint}</p>
	{/if}
</div>
