<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLSelectAttributes } from 'svelte/elements';

	// The single definition of a FORM select (task-151).
	//
	// A native <select> ignores most of the app's field classes: the browser
	// paints its own arrow and control height, so beside an <input> carrying the
	// identical classes it reads as a different, heavier control — and
	// differently again per platform. `appearance-none` plus our own chevron
	// makes it match.
	//
	// Deliberately NOT used by the five in-cell selects (EstimationGrid,
	// AdditionalCostsPanel, the bucket EditorModule), which are `bg-transparent`
	// and borderless because they sit inside a grid cell where a bordered box
	// would be wrong, nor by UserMenu's chrome select. See src/frontend/CLAUDE.md.
	//
	// `{...rest}` forwarding is load-bearing, as it is for Button: call sites
	// pass `id`, `onchange`, `disabled` and `aria-*`, and `bind:value` needs the
	// real element.
	let {
		value = $bindable(''),
		class: extraClass = '',
		children,
		...rest
	}: HTMLSelectAttributes & { children?: Snippet } = $props();

	// An unset value is a placeholder, not a choice — grey it so the field reads
	// as empty the way a text input's placeholder does.
	const cls = $derived(
		[
			'w-full appearance-none border rounded pl-3 pr-9 py-2 text-sm bg-white',
			'focus:outline-none focus:ring-1 focus:ring-brand-green/40',
			value === '' ? 'text-gray-400' : 'text-gray-900',
			extraClass
		]
			.filter(Boolean)
			.join(' ')
	);
</script>

<div class="relative">
	<select bind:value class={cls} {...rest}>
		{@render children?.()}
	</select>
	<!-- Decoration only: the select is already announced, and pointer-events-none
	     keeps the chevron from swallowing the click that opens it. -->
	<svg
		class="pointer-events-none absolute right-3 inset-y-0 my-auto h-4 w-4 text-gray-400"
		viewBox="0 0 20 20"
		fill="none"
		stroke="currentColor"
		stroke-width="1.75"
		aria-hidden="true"
	>
		<path d="M6 8l4 4 4-4" stroke-linecap="round" stroke-linejoin="round" />
	</svg>
</div>
