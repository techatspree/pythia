<script lang="ts" module>
	import Button from '#lib/ui/Button.svelte';
	// Body of a successful merge into Merlin (task-223): the merged document as
	// base64 plus the report of every change it made. Paths are WBS paths.
	export type MerlinMergeDependency = { fromPath: string; toPath: string };
	export type MerlinMergeReport = {
		effortChanges: { path: string; beforePT: number | null; afterPT: number | null }[];
		activitiesAdded: string[];
		activitiesRemoved: string[];
		activitiesMoved: string[];
		dependenciesAdded: MerlinMergeDependency[];
		dependenciesRemoved: MerlinMergeDependency[];
		dependenciesSkipped: (MerlinMergeDependency & { reason: 'WOULD_CREATE_CYCLE' | 'PARENT_CHILD' })[];
		contentRemoved: {
			assignments: number;
			comments: number;
			attachments: number;
			costs: number;
			customAttributeValues: number;
		};
	};
	export type MerlinMergeResult = { fileName: string; document: string; report: MerlinMergeReport };
</script>

<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatNumber, DEFAULT_LOCALE } from '#lib/format.ts';

	// Shown after the merged file was downloaded. Informational only, so the one
	// button is focused on open and ESC closes it too.
	let { result, onclose }: { result: MerlinMergeResult; onclose: () => void } = $props();

	let closeButton = $state<HTMLButtonElement | null>(null);
	$effect(() => {
		closeButton?.focus();
	});

	const report = $derived(result.report);

	function effort(value: number | null): string {
		return value == null
			? $_('merlinMerge.noEffort')
			: formatNumber(value, $locale ?? DEFAULT_LOCALE, { maximumFractionDigits: 2 });
	}

	function dependency(d: MerlinMergeDependency): string {
		return $_('merlinMerge.dependency', { values: { from: d.fromPath, to: d.toPath } });
	}

	// The fixed order: structural changes first (they explain the rest), the
	// long effort list last. An empty section is not rendered.
	const sections = $derived(
		[
			{ key: 'activities-added', label: $_('merlinMerge.activitiesAdded'), lines: report.activitiesAdded },
			{ key: 'activities-removed', label: $_('merlinMerge.activitiesRemoved'), lines: report.activitiesRemoved },
			{ key: 'activities-moved', label: $_('merlinMerge.activitiesMoved'), lines: report.activitiesMoved },
			{
				key: 'dependencies-removed',
				label: $_('merlinMerge.dependenciesRemoved'),
				lines: report.dependenciesRemoved.map(dependency)
			},
			{
				key: 'dependencies-added',
				label: $_('merlinMerge.dependenciesAdded'),
				lines: report.dependenciesAdded.map(dependency)
			},
			{
				key: 'dependencies-skipped',
				label: $_('merlinMerge.dependenciesSkipped'),
				lines: report.dependenciesSkipped.map((d) =>
					$_('merlinMerge.skippedDependency', {
						values: { from: d.fromPath, to: d.toPath, reason: $_(`merlinMerge.skipReason.${d.reason}`) }
					})
				)
			},
			{
				key: 'content-removed',
				label: $_('merlinMerge.contentRemoved'),
				counted: false,
				lines: (
					[
						['assignments', report.contentRemoved.assignments],
						['comments', report.contentRemoved.comments],
						['attachments', report.contentRemoved.attachments],
						['costs', report.contentRemoved.costs],
						['customAttributeValues', report.contentRemoved.customAttributeValues]
					] as const
				)
					.filter(([, count]) => count > 0)
					.map(([kind, count]) => $_(`merlinMerge.${kind}`, { values: { count } }))
			},
			{
				key: 'efforts',
				label: $_('merlinMerge.efforts'),
				lines: report.effortChanges.map((c) => c.path),
				details: report.effortChanges.map((c) =>
					$_('merlinMerge.effortChange', {
						values: { before: effort(c.beforePT), after: effort(c.afterPT) }
					})
				)
			}
		].filter((s) => s.lines.length > 0) as {
			key: string;
			label: string;
			lines: readonly string[];
			counted?: boolean;
			details?: string[];
		}[]
	);

	const empty = $derived(sections.length === 0);
</script>

<svelte:window
	onkeydown={(e) => {
		if (e.key === 'Escape') onclose();
	}}
/>

<div
	class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
	role="dialog"
	aria-modal="true"
	aria-label={$_('merlinMerge.title')}
	data-testid="merlin-merge-report.dialog"
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-lg">
		<h2 class="text-lg font-semibold mb-4">{$_('merlinMerge.title')}</h2>
		{#if empty}
			<p class="text-sm text-ink-muted mb-4">
				{$_('merlinMerge.introNoChanges', { values: { fileName: result.fileName } })}
			</p>
			<p class="text-sm text-ink-body mb-4" data-testid="merlin-merge-report.empty">
				{$_('merlinMerge.empty')}
			</p>
		{:else}
			<p class="text-sm text-ink-muted mb-1">
				{$_('merlinMerge.intro', { values: { fileName: result.fileName } })}
			</p>
			<p class="text-sm text-ink-muted mb-4">{$_('merlinMerge.addOnlyNote')}</p>

			{#each sections as section (section.key)}
				<div class="mb-3">
					<h3 class="text-xs font-semibold uppercase tracking-wide text-ink-muted mb-1">
						{section.label}{section.counted === false ? '' : ` (${section.lines.length})`}
					</h3>
					<ul
						class="max-h-32 overflow-y-auto border rounded bg-surface-subtle/60 text-xs text-ink-body"
						data-testid={`merlin-merge-report.section.${section.key}`}
					>
						{#each section.lines as line, i (i)}
							<li class="px-2 py-1 border-b last:border-b-0 break-words flex justify-between gap-2">
								<span>{line}</span>
								{#if section.details}<span class="whitespace-nowrap">{section.details[i]}</span>{/if}
							</li>
						{/each}
					</ul>
				</div>
			{/each}
		{/if}

		<div class="flex justify-end gap-2 mt-4">
			<Button bind:element={closeButton} data-testid="merlin-merge-report.close" onclick={onclose}
				>{$_('merlinMerge.close')}</Button
			>
		</div>
	</div>
</div>
