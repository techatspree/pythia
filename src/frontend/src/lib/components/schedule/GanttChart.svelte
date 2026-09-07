<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import Button from '$lib/ui/Button.svelte';
	import { formatFixed, formatDate, DEFAULT_LOCALE } from '$lib/format';
	import { downloadResponse } from '$lib/api/download';
	import { log } from '$lib/log';
	import type { ProjectScheduleView, ScheduledTaskView } from '$lib/adapter';

	// Renders the levelled plan as bars and exports it as Mermaid text
	// (task-158). Computes NOTHING about the schedule: every date and duration
	// comes from the domain via `ProjectScheduleView`.
	//
	// `ProjectScheduleView` is the FRONTEND type from `$lib/adapter`;
	// `ProjectSchedule` is the Kotlin name and does not exist in TypeScript.
	let {
		schedule,
		title = '',
		variableItems = [],
		onerror
	}: {
		schedule: ProjectScheduleView | null;
		title?: string;
		/**
		 * Accompanying work (`h/Woche`): project management, UX support. These are
		 * NOT scheduled tasks (task-177) — their effort derives from their phase's
		 * length, so scheduling them would close a cycle. They are drawn across
		 * the whole of their phase's window instead, which makes them parallel to
		 * everything in the phase by construction rather than by a layout case.
		 */
		variableItems?: { logicalId: string; title: string; phaseAbbreviation: string | null }[];
		onerror?: (message: string) => void;
	} = $props();

	const INDENT = 16;
	const TICKS = 5;

	const tasks = $derived(schedule?.tasks ?? []);
	const span = $derived(Math.max(schedule?.projectDurationDays ?? 0, 0.0001));

	// The axis is in WORKING DAYS, like the domain. Calendar dates appear only
	// in the export.
	const ticks = $derived(
		Array.from({ length: TICKS + 1 }, (_v, i) => (span * i) / TICKS)
	);

	function pct(v: number): number {
		return (v / span) * 100;
	}

	function days(v: number): string {
		return formatFixed(v, $locale ?? DEFAULT_LOCALE, 1);
	}

	/**
	 * A group's bar is its rolled-up SPAN (`earliestFinish - earliestStart`),
	 * which is deliberately not an effort quotient and will not equal the sum of
	 * its children when they overlap (task-164).
	 */
	function barOf(t: ScheduledTaskView): { left: number; width: number } {
		return { left: pct(t.earliestStart), width: Math.max(pct(t.earliestFinish - t.earliestStart), 0.5) };
	}

	const windowOf = (abbr: string | null) =>
		abbr == null ? null : (schedule?.phaseWindows ?? []).find((w) => w.abbreviation === abbr) ?? null;

	/** Only the accompanying items whose phase actually has scheduled work. */
	const variableRows = $derived(
		variableItems
			.map((v) => ({ item: v, win: windowOf(v.phaseAbbreviation) }))
			.filter((r) => r.win != null && r.win.scheduledLeafCount > 0)
	);

	function rowAria(t: ScheduledTaskView): string {
		// The critical distinction is carried in TEXT as well as colour.
		const key = t.onCriticalPath ? 'schedule.gantt.barCriticalAria' : 'schedule.gantt.barAria';
		return $_(key, {
			values: { title: t.title, start: days(t.earliestStart), days: days(t.durationDays) }
		});
	}

	// ── Export ───────────────────────────────────────────────────────────────
	// Working days become calendar dates HERE and only here: it is a
	// presentation concern, so it stays out of the domain. Weekends are skipped;
	// holidays are deliberately not modelled.
	// An ISO day string, never a Date object: `svelte/prefer-svelte-reactivity`
	// rightly objects to a mutable Date, and none is needed here.
	let startDate = $state(todayIso());

	const DAY_MS = 86_400_000;

	function todayIso(): string {
		return new Date(Date.now()).toISOString().slice(0, 10);
	}

	/** UTC day of week, 0 = Sunday. Read-only — nothing is mutated. */
	function dayOfWeek(epochMs: number): number {
		return new Date(epochMs).getUTCDay();
	}

	function isWeekend(epochMs: number): boolean {
		const d = dayOfWeek(epochMs);
		return d === 0 || d === 6;
	}

	/**
	 * Working-day offset → ISO calendar date, skipping Saturdays and Sundays.
	 * Pure epoch arithmetic rather than `Date.setDate`, so no mutable Date
	 * exists to be reactive about. Holidays are deliberately not modelled.
	 */
	function addWorkingDays(fromIso: string, offset: number): string {
		let t = Date.parse(`${fromIso}T00:00:00Z`);
		// If the chosen start itself falls on a weekend, walk to the next Monday
		// so working day 0 is a working day.
		while (isWeekend(t)) t += DAY_MS;
		let left = Math.round(offset);
		while (left > 0) {
			t += DAY_MS;
			if (!isWeekend(t)) left -= 1;
		}
		return new Date(t).toISOString().slice(0, 10);
	}

	/** A Mermaid task name and id cannot carry `:` or a newline. */
	function safe(s: string): string {
		return s.replace(/[:\n\r]+/g, ' ').trim();
	}

	function buildMermaid(): string {
		const lines = [
			'gantt',
			`    title ${safe(title || $_('schedule.gantt.title'))}`,
			'    dateFormat YYYY-MM-DD',
			'    axisFormat %d.%m'
		];
		for (const [i, t] of tasks.entries()) {
			if (t.isGroup) {
				lines.push(`    section ${safe(t.title)}`);
				continue;
			}
			const start = addWorkingDays(startDate, t.earliestStart);
			const dur = Math.max(1, Math.round(t.durationDays));
			const tag = t.onCriticalPath ? 'crit, ' : '';
			lines.push(`    ${safe(t.title)} :${tag}t${i}, ${start}, ${dur}d`);
		}
		return lines.join('\n') + '\n';
	}

	function exportMermaid() {
		try {
			const blob = new Blob([buildMermaid()], { type: 'text/vnd.mermaid;charset=utf-8' });
			downloadResponse(blob, null, $_('schedule.gantt.fileName'));
			log.debug(`gantt: exported ${tasks.length} task(s) as Mermaid`);
		} catch (e: unknown) {
			// Both, per the CLAUDE.md frontend rules: a catch that only logs is
			// not acceptable.
			log.error('gantt: Mermaid export failed', e);
			onerror?.($_('schedule.gantt.exportFailed'));
		}
	}
</script>

{#if schedule?.error != null}
	<!-- Either error kind replaces the chart: a Gantt drawn from an empty task
	     list would read as "zero days". -->
	<p class="text-sm text-amber-900" data-testid="gantt-error">
		{schedule.error.kind === 'CYCLE'
			? $_('schedule.error.cycle')
			: $_('schedule.error.invalidTeamFte')}
	</p>
{:else if tasks.length === 0}
	<p class="text-sm text-ink-muted" data-testid="gantt-empty">{$_('schedule.editor.empty')}</p>
{:else}
	<div data-testid="gantt-chart">
		<div class="mb-3 flex flex-wrap items-end gap-3">
			<div>
				<label class="mb-1 block text-sm font-medium" for="gantt-start"
					>{$_('schedule.gantt.startDate')}</label
				>
				<input
					id="gantt-start"
					type="date"
					class="rounded border border-hairline px-2 py-1 text-sm focus:border-brand-green focus:ring-1 focus:ring-brand-green/40 focus:outline-none"
					data-testid="gantt-start-date"
					bind:value={startDate}
				/>
			</div>
			<Button variant="secondary" size="sm" data-testid="gantt-export" onclick={exportMermaid}>
				{$_('schedule.gantt.export')}
			</Button>
			<p class="text-xs text-ink-muted">{$_('schedule.gantt.exportHint')}</p>
		</div>

		<!-- Horizontal overflow scrolls INSIDE the component rather than widening
		     the page (the TreeTable lesson). -->
		<div class="overflow-x-auto">
			<div class="min-w-[40rem]">
				<div class="relative mb-1 h-4 text-xs text-ink-faint" aria-hidden="true">
					{#each ticks as t (t)}
						<span class="absolute -translate-x-1/2" style="left: {pct(t)}%">{days(t)}</span>
					{/each}
				</div>
				<p class="mb-2 text-xs text-ink-muted">{$_('schedule.gantt.axis')}</p>

				{#each tasks as t (t.logicalId)}
					{@const bar = barOf(t)}
					<div class="flex items-center gap-2 py-0.5" data-testid="gantt-row">
						<span
							class="w-56 shrink-0 truncate text-sm {t.isGroup ? 'font-medium' : ''}"
							style="padding-left: {t.depth * INDENT}px"
							title={t.title}>{t.title}</span
						>
						<div class="relative h-4 flex-1 rounded bg-surface-subtle">
							<div
								class="absolute h-4 rounded {t.onCriticalPath
									? 'bg-brand-green'
									: 'bg-brand-green/30'}"
								style="left: {bar.left}%; width: {bar.width}%"
								role="img"
								aria-label={rowAria(t)}
								data-testid={t.onCriticalPath ? 'gantt-bar-critical' : 'gantt-bar'}
							></div>
						</div>
						<span class="w-16 shrink-0 text-right text-xs text-ink-muted"
							>{days(t.durationDays)}</span
						>
					</div>
				{/each}
				{#each variableRows as row (row.item.logicalId)}
					<div class="flex items-center gap-2 py-0.5" data-testid="gantt-variable-row">
						<span class="w-56 shrink-0 truncate text-sm italic" title={row.item.title}
							>{row.item.title}</span
						>
						<div class="relative h-4 flex-1 rounded bg-surface-subtle">
							<!-- Surface AND edge differ from a scheduled bar (task-174's rule):
							     a hatched outline rather than a solid fill, so the distinction
							     survives without colour. -->
							<div
								class="absolute h-4 rounded border-2 border-dashed border-brand-green/70 bg-brand-green/10"
								style="left: {pct(row.win!.earliestStart)}%; width: {Math.max(
									pct(row.win!.earliestFinish - row.win!.earliestStart),
									0.5
								)}%"
								role="img"
								aria-label={$_('schedule.gantt.accompanyingAria', {
									values: { title: row.item.title, phase: row.item.phaseAbbreviation ?? '' }
								})}
								data-testid="gantt-bar-accompanying"
							></div>
						</div>
						<span class="w-16 shrink-0 text-right text-xs text-ink-muted"
							>{$_('schedule.gantt.accompanyingShort')}</span
						>
					</div>
				{/each}
			</div>
		</div>

		<p class="mt-2 text-xs text-ink-faint">
			{$_('schedule.gantt.startsOn', {
				values: { date: formatDate(startDate, $locale ?? DEFAULT_LOCALE, { dateStyle: 'medium' }) }
			})}
		</p>
	</div>
{/if}
