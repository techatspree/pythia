<script module lang="ts">
	import { get } from 'svelte/store';
	import { _ } from 'svelte-i18n';

	// Relative-time formatter, shared with the editor page's toolbar tooltips.
	// Presentation only — no business logic. Reads the i18n store imperatively
	// via get(_) because this is a module-context function, not a component.
	export function relativeTime(iso: string | null | undefined): string {
		if (!iso) return '';
		const then = new Date(iso).getTime();
		if (Number.isNaN(then)) return '';
		const secs = Math.round((Date.now() - then) / 1000);
		if (secs < 60) return get(_)('history.relativeJustNow');
		const mins = Math.round(secs / 60);
		if (mins < 60) return get(_)('history.relativeMinutes', { values: { mins } });
		const hours = Math.round(mins / 60);
		if (hours < 24) return get(_)('history.relativeHours', { values: { hours } });
		const days = Math.round(hours / 24);
		return get(_)('history.relativeDays', { values: { days } });
	}
</script>

<script lang="ts">
	import { onMount } from 'svelte';
	import { locale } from 'svelte-i18n';
	import type { components } from '$lib/api/schema';
	import { formatNumber, DEFAULT_LOCALE } from '$lib/format';
	import { log } from '$lib/log';

	type MutationLogEntryDto = components['schemas']['MutationLogEntryDto'];
	type ChangeSummaryDto = components['schemas']['ChangeSummaryDto'];

	let { history }: { history: MutationLogEntryDto[] } = $props();

	// Newest-first without mutating the store's array.
	const ordered = $derived([...history].sort((a, b) => b.sequenceNumber - a.sequenceNumber));

	onMount(() => log.debug(`UndoHistoryPanel: ${history.length} entries`));

	// The domain ships locale-NEUTRAL values (task-110). Format numeric ones per
	// the active locale; pass non-numeric strings (titles, abbreviations) through.
	function numberOrText(v: string | null | undefined): string {
		if (v == null || v === '') return '';
		const n = Number(v);
		return Number.isFinite(n) ? formatNumber(n, $locale ?? DEFAULT_LOCALE) : v;
	}

	// One summary line, fully internationalised: `history.change.<kind>` with the
	// field label resolved via `history.field.<field>`.
	function summaryLine(line: ChangeSummaryDto): string {
		return $_(`history.change.${line.kind}`, {
			values: {
				path: line.path ?? '',
				field: line.field ? $_(`history.field.${line.field}`) : '',
				oldValue: numberOrText(line.oldValue),
				newValue: numberOrText(line.newValue),
				remaining: line.remaining ?? 0
			}
		});
	}
</script>

<div class="mt-4 border rounded-lg">
	<div class="px-4 py-2 border-b bg-gray-50 text-sm font-semibold text-gray-700">{$_('history.title')}</div>
	{#if ordered.length === 0}
		<p class="px-4 py-3 text-sm text-gray-400">{$_('history.empty')}</p>
	{:else}
		<ul class="divide-y">
			{#each ordered as entry (entry.id)}
				<li class="px-4 py-2 text-sm">
					<div class="flex items-center justify-between">
						<div class="min-w-0 truncate">
							<span class="font-medium text-gray-800">{entry.userDisplayName}</span>
							<span class="text-gray-400"> · {relativeTime(entry.createdAt)}</span>
						</div>
						{#if entry.status === 'ACTIVE'}
							<span
								class="shrink-0 ml-2 px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green"
								>{$_('history.statusActive')}</span
							>
						{:else}
							<span class="shrink-0 ml-2 px-2 py-0.5 text-xs rounded-full bg-gray-200 text-gray-500"
								>{$_('history.statusUndone')}</span
							>
						{/if}
					</div>
					{#if entry.summary.length > 0}
						<ul class="mt-1 pl-3 text-xs text-gray-600 space-y-0.5">
							{#each entry.summary.slice(0, 5) as line, i (i)}
								<li>· {summaryLine(line)}</li>
							{/each}
						</ul>
						{#if entry.summary.length > 5}
							<details class="mt-1 pl-3 text-xs">
								<summary class="cursor-pointer text-gray-500">{$_('history.change.showMore')}</summary>
								<ul class="mt-1 text-gray-600 space-y-0.5">
									{#each entry.summary.slice(5) as line, i (i)}
										<li>· {summaryLine(line)}</li>
									{/each}
								</ul>
							</details>
						{/if}
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</div>
