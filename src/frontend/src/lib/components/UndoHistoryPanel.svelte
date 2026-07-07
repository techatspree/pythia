<script module lang="ts">
	// German relative-time formatter, shared with the editor page's toolbar
	// tooltips. Presentation only — no business logic.
	export function relativeTime(iso: string | null | undefined): string {
		if (!iso) return '';
		const then = new Date(iso).getTime();
		if (Number.isNaN(then)) return '';
		const secs = Math.round((Date.now() - then) / 1000);
		if (secs < 60) return 'gerade eben';
		const mins = Math.round(secs / 60);
		if (mins < 60) return `vor ${mins} min`;
		const hours = Math.round(mins / 60);
		if (hours < 24) return `vor ${hours} h`;
		const days = Math.round(hours / 24);
		return `vor ${days} d`;
	}
</script>

<script lang="ts">
	import type { components } from '$lib/api/schema';

	type MutationLogEntryDto = components['schemas']['MutationLogEntryDto'];

	let { history }: { history: MutationLogEntryDto[] } = $props();

	// Newest-first without mutating the store's array.
	const ordered = $derived([...history].sort((a, b) => b.sequenceNumber - a.sequenceNumber));
</script>

<div class="mt-4 border rounded-lg">
	<div class="px-4 py-2 border-b bg-gray-50 text-sm font-semibold text-gray-700">Verlauf</div>
	{#if ordered.length === 0}
		<p class="px-4 py-3 text-sm text-gray-400">Noch keine Änderungen.</p>
	{:else}
		<ul class="divide-y">
			{#each ordered as entry (entry.id)}
				<li class="flex items-center justify-between px-4 py-2 text-sm">
					<div class="min-w-0 truncate">
						<span class="font-medium text-gray-800">{entry.kind}</span>
						<span class="text-gray-500"> · {entry.userDisplayName}</span>
						<span class="text-gray-400"> · {relativeTime(entry.createdAt)}</span>
					</div>
					{#if entry.status === 'ACTIVE'}
						<span
							class="shrink-0 ml-2 px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green"
							>Aktiv</span
						>
					{:else}
						<span class="shrink-0 ml-2 px-2 py-0.5 text-xs rounded-full bg-gray-200 text-gray-500"
							>Rückgängig gemacht</span
						>
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</div>
