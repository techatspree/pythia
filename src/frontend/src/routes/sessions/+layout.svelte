<script lang="ts">
	// The session route group has NO chrome of its own (task-141). It used to
	// substitute its own top bar for the app header, which left the room looking
	// unlike the rest of the app with no recognisable way back; the global
	// AppHeader in the root layout now navigates here like everywhere else. This
	// layout only supplies the room background, which ends with the content.
	// NOT `min-h-screen`: that was right while this layout WAS the whole screen,
	// but stacked under the global header it pushes every session page past the
	// fold and leaves a permanent scrollbar over a strip of dead space.
	let { children } = $props();
</script>

<div class="bg-gray-50">
	{@render children()}
</div>
