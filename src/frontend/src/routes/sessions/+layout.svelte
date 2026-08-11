<script lang="ts">
	// The session route group has NO chrome and NO background of its own. It
	// once substituted its own top bar for the app header (removed by task-141)
	// and then kept a grey room background (removed by task-148) — both made
	// the room look unlike the rest of the app. Session pages now use the same
	// white surface and full-width `p-6` wrapper as every other route, so this
	// layout is a pass-through. Do not paint a page background here again.
	//
	// Two traps if you are tempted to make the room fill the viewport:
	//   - NOT `min-h-screen` here: stacked under the global header it pushes
	//     every session page past the fold and leaves a permanent scrollbar
	//     over dead space.
	//   - Do NOT make the root `<main>` a flex container so this wrapper can
	//     `flex-1`: that turns every page's root element into a flex item,
	//     whose automatic minimum size stops the TreeTable's horizontal scroll
	//     wrapper from overflowing. `e2e/treetable-responsive.test.ts` catches
	//     it, and `h-full` does not resolve there either.
	let { children } = $props();
</script>

{@render children()}
