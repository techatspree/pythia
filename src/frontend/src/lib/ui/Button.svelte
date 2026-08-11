<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLButtonAttributes } from 'svelte/elements';

	// The SINGLE definition of a button (task-149). These class strings used to be
	// retyped at 20+ sites, which is how the session UI drifted out of style
	// (task-148) — and the drift was invisible in review because nothing broke.
	//
	// `{...rest}` forwarding is load-bearing, not incidental: the Playwright suite
	// selects on `data-testid`, and call sites pass `disabled`, `title`, `aria-*`
	// and `onclick`. A version that drops rest props type-checks fine and then
	// fails e2e broadly.
	type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
	// Real sizes in use. `md` is the default; `sm`/`xs` are the compact
	// in-table/in-panel actions. Without these the colour treatment stayed
	// duplicated at six sites that differ only in padding.
	type Size = 'xs' | 'sm' | 'md';

	// `element` exposes the underlying DOM node. Three modal dialogs focus their
	// cancel/reload button on open — `bind:this` on a component yields the
	// component instance, not the element, so that a11y behaviour needs an
	// explicit binding: `bind:element={cancelButton}`.
	let {
		variant = 'primary',
		size = 'md',
		href = undefined,
		type = 'button',
		class: extraClass = '',
		element = $bindable(null),
		children,
		...rest
	}: HTMLButtonAttributes & {
		variant?: Variant;
		size?: Size;
		// When set, renders an <a> instead — a link that looks like a button
		// (e.g. "compare versions", "start estimation session"). Same treatment,
		// correct semantics and keyboard behaviour for navigation.
		href?: string;
		element?: HTMLButtonElement | null;
		children?: Snippet;
	} = $props();

	const SIZES: Record<Size, string> = {
		md: 'px-4 py-2 text-sm',
		sm: 'px-3 py-1.5 text-sm',
		xs: 'px-3 py-1 text-xs'
	};

	// `disabled:opacity-50` is applied unconditionally — it is inert on an enabled
	// button, and its ad-hoc presence (7 sites) vs absence (17) was itself drift.
	const VARIANTS: Record<Variant, string> = {
		primary: 'bg-brand-green text-white rounded hover:bg-brand-green-hover',
		secondary: 'border rounded hover:bg-gray-50',
		ghost: 'text-gray-500 hover:text-gray-700',
		danger: 'bg-red-600 text-white rounded hover:bg-red-700'
	};

	const cls = $derived(
		[SIZES[size], VARIANTS[variant], 'disabled:opacity-50', extraClass].filter(Boolean).join(' ')
	);

	// `rest` is typed for a <button>; the anchor branch needs it widened, since
	// the event-handler types are element-specific (ClipboardEventHandler
	// <HTMLButtonElement> vs <HTMLAnchorElement>).
	const anchorRest = $derived(rest as Record<string, unknown>);
</script>

{#if href}
	<!-- The primitive receives an ALREADY-resolved href (callers build it with
	     resolve()); it cannot resolve a route it does not know. Same scoped
	     suppression the other computed-href sites carry — and, like those, the
	     <a must stay on ONE line or the disable lands on the wrong line. -->
	<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
	<a {href} class={cls} {...anchorRest}>{@render children?.()}</a>
{:else}
	<button bind:this={element} {type} class={cls} {...rest}>{@render children?.()}</button>
{/if}
