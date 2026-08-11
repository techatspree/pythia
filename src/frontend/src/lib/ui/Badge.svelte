<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLAttributes } from 'svelte/elements';

	// The chip (task-154). Hand-rolled at 11 sites across 4 class strings before
	// this existed.
	//
	// The variants carry MEANING, not just colour: `brand` is the app's
	// "current / active" signal (session status, the chosen method, the current
	// version), so do not reach for it merely because a chip should stand out.
	type Variant = 'brand' | 'neutral' | 'muted' | 'warn';

	let {
		variant = 'neutral',
		class: extraClass = '',
		children,
		...rest
	}: HTMLAttributes<HTMLSpanElement> & { variant?: Variant; children?: Snippet } = $props();

	const VARIANTS: Record<Variant, string> = {
		brand: 'bg-brand-green/20 text-brand-green',
		neutral: 'bg-gray-100 text-gray-500',
		muted: 'bg-gray-200 text-gray-500',
		// Draft / not-yet-final. A real state in VersionList, not decoration —
		// without it that site would need a per-call class override, which is the
		// duplication this primitive removes.
		warn: 'bg-yellow-100 text-yellow-800'
	};

	// `{...rest}` forwarding, as for Button: call sites pass `data-testid` and
	// `title`.
	const cls = $derived(
		['px-2 py-0.5 text-xs rounded-full', VARIANTS[variant], extraClass].filter(Boolean).join(' ')
	);
</script>

<span class={cls} {...rest}>{@render children?.()}</span>
