<script lang="ts">
	import type { HTMLAttributes } from 'svelte/elements';

	// The SINGLE definition of the collapse/expand indicator (task-168).
	//
	// The glyph pair is U+25BC / U+25B6 — the FULL-SIZE triangles. Five of the
	// seven adopting sites already used these; the two the schedule work added
	// used the SMALL variants U+25BE / U+25B8, which render visibly lighter at
	// the same font size and so read as a different control. Five versus two
	// made the full-size pair the established one.
	//
	// An SVG chevron was considered and deliberately deferred: `$lib/ui/Select`
	// draws one, and an SVG would render more consistently than a text glyph
	// (whose weight and baseline depend on the font's coverage) — but switching
	// changes how five existing toggles LOOK, which is a redesign rather than
	// "make them uniform". Behind this primitive it becomes a one-file change.
	//
	// `aria-hidden` defaults to true because every adopting site already labels
	// its own control; `{...rest}` lets a caller override that, or attach a
	// `data-testid` the Playwright suite selects on.
	let {
		expanded,
		class: extraClass = '',
		...rest
	}: HTMLAttributes<HTMLSpanElement> & {
		expanded: boolean;
		class?: string;
	} = $props();
</script>

<span aria-hidden="true" class={extraClass} {...rest}>{expanded ? '▼' : '▶'}</span>
