<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { _, locale } from 'svelte-i18n';
	import { formatFixed } from '$lib/format';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';
	import Page from '$lib/ui/Page.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import DependencyEditor from '$lib/components/schedule/DependencyEditor.svelte';
	import GanttChart from '$lib/components/schedule/GanttChart.svelte';
	import { computeEstimation, type ScheduleEdge } from '$lib/adapter.js';
	import { normalizeRoots, labelsByLogicalId, type Node } from '$lib/estimationNodes';
	import type { ApiVersionResponse } from '$lib/api/types.js';

	// The dependency graph on its own route (task-167). It was a collapsible
	// section in the version editor, which is the wrong shape for a working
	// surface that needs the full viewport and scrolls horizontally by layer.
	//
	// This route cannot share the version editor's $state, so it owns its own
	// load → edit → PUT cycle. The PUT is a PARTIAL DraftUpdateDto — only
	// { teamFte, dependencies } — because every field there is optional and a
	// null field means "leave unchanged" (task-156). Sending roots or costs from
	// here would overwrite edits made in the version editor.
	let versionData = $state<ApiVersionResponse | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);
	let saveStatus = $state<'idle' | 'saving' | 'saved'>('idle');

	let currentTeamFte = $state(1);
	let currentDependencies = $state<ScheduleEdge[]>([]);
	// Roots are loaded read-only: the schedule is computed from them, but this
	// page never edits or sends them.
	let currentRoots = $state<unknown[]>([]);
	let currentDailyRate = $state(800);
	let currentStdDevFactor = $state(2);
	let currentSalesSurcharge = $state(0.1);
	let currentDrivers = $state<{ description: string; factor: number; comment: string }[]>([]);
	let currentPhases = $state<{ name: string; abbreviation: string; durationWeeks: number | null }[]>(
		[]
	);

	let lastSavedSnapshot: string | null = null;
	let saveTimer: ReturnType<typeof setTimeout> | null = null;

	const editable = $derived(versionData?.isDraft === true);

	// Display names for the dependency rows, taken from the TREE rather than the
	// schedule: a failed schedule comes back with an empty `tasks` list, so the
	// cycle notice — the one place that most needs to name an item — is exactly
	// where the schedule can name nothing (task-171).
	const labels = $derived(labelsByLogicalId(currentRoots as Node[]));

	/**
	 * Accompanying work, read from the TREE rather than the schedule: task-177
	 * excludes these leaves from the plan entirely, so they never appear in
	 * `schedule.tasks`.
	 */
	function collectVariable(
		nodes: Node[]
	): { logicalId: string; title: string; phaseAbbreviation: string | null }[] {
		const out: { logicalId: string; title: string; phaseAbbreviation: string | null }[] = [];
		for (const n of nodes) {
			if (n.type === 'GROUP') out.push(...collectVariable(n.children));
			else if (n.type === 'TIME_RELATIVE')
				out.push({
					logicalId: n.logicalId,
					title: n.description,
					phaseAbbreviation: n.phaseAbbreviation ?? null
				});
		}
		return out;
	}

	const variableItems = $derived(collectVariable(currentRoots as Node[]));

	/**
	 * Would this candidate edge list close a loop? Returns the logical ids the
	 * domain considers involved, or null when the list is fine (task-172).
	 *
	 * Runs the SAME `computeEstimation` the `estimation` derivation above uses,
	 * with the candidate substituted — cycle detection is domain logic, and a
	 * drawn edge is not a graph edge (a group edge lowers to milestone nodes), so
	 * a hand-rolled traversal here would disagree with the thing that decides.
	 * One extra build+calculate per DROP is fine; a drop is human-paced.
	 */
	function cycleCheck(candidate: ScheduleEdge[]): string[] | null {
		try {
			const probe = computeEstimation(
				currentRoots as never,
				{
					dailyRate: currentDailyRate,
					stdDevFactor: currentStdDevFactor,
					salesSurcharge: currentSalesSurcharge
				},
				currentDrivers,
				currentPhases,
				[],
				{ dependencies: candidate, teamFte: currentTeamFte }
			);
			const err = probe.schedule?.error;
			return err?.kind === 'CYCLE' ? err.involvedLogicalIds : null;
		} catch (e: unknown) {
			// A failed probe must never block a legitimate edit: fall through to
			// "no cycle" and let the committed schedule report the problem, which
			// is the behaviour that existed before this check.
			log.error('schedule: cycle probe failed; allowing the edge', e);
			return null;
		}
	}

	// The schedule comes off the domain, from the same single build+calculate
	// the version editor uses — this page computes no numbers of its own.
	const estimation = $derived.by(() => {
		try {
			return computeEstimation(
				currentRoots as never,
				{
					dailyRate: currentDailyRate,
					stdDevFactor: currentStdDevFactor,
					salesSurcharge: currentSalesSurcharge
				},
				currentDrivers,
				currentPhases,
				[],
				{ dependencies: currentDependencies, teamFte: currentTeamFte }
			);
		} catch (e: unknown) {
			const msg = e instanceof Error ? e.message : String(e);
			log.error('schedule computation failed:', e);
			bannerMessage = $_('editor.calculationFailed', { values: { message: msg } });
			return null;
		}
	});
	const schedule = $derived(estimation?.schedule ?? null);

	/** Only the two fields this route owns, so an unrelated edit cannot trigger a PUT. */
	function editableSnapshot(): string {
		return JSON.stringify({
			teamFte: currentTeamFte,
			dependencies: $state.snapshot(currentDependencies)
		});
	}

	async function loadVersion() {
		loading = true;
		bannerMessage = null;
		try {
			const estimationId = page.params.id;
			const versionNumber = page.params.versionNumber;
			const isDraft = page.url.searchParams.get('draft') === 'true' || versionNumber === 'draft';
			const url = isDraft
				? `/api/estimations/${estimationId}/versions/draft`
				: `/api/estimations/${estimationId}/versions/${versionNumber}`;
			const res = await apiFetch(url);
			await assertOk(res, $_('editor.loadFailed'));
			const data: ApiVersionResponse = await res.json();
			versionData = data;
			currentRoots = normalizeRoots(data) as unknown[];
			currentDailyRate = data.dailyRate ?? 800;
			currentStdDevFactor = data.stdDevFactor ?? 2.0;
			currentSalesSurcharge = data.salesSurcharge ?? 0.1;
			// `data` is already typed as ApiVersionResponse — let TS infer the
			// element types rather than restating them inline (a hand-rolled shape
			// disagreed with ApiEffortDriver.comment being `string | null`).
			currentDrivers = (data.effortDrivers ?? []).map((d) => ({
				description: d.description ?? '',
				factor: d.factor ?? 0,
				comment: d.comment ?? ''
			}));
			currentPhases = (data.phases ?? []).map((p) => ({
				name: p.name ?? '',
				abbreviation: p.abbreviation ?? '',
				durationWeeks: p.durationWeeks ?? null
			}));
			currentTeamFte = (data as { teamFte?: number }).teamFte ?? 1;
			currentDependencies = (
				(data as { dependencies?: { fromLogicalId: string; toLogicalId: string }[] }).dependencies ??
				[]
			).map((d) => ({ fromLogicalId: d.fromLogicalId, toLogicalId: d.toLogicalId }));
			// Baseline AFTER hydration: the same guard the version editor uses, so
			// loading never triggers a save.
			lastSavedSnapshot = editableSnapshot();
		} catch (e: unknown) {
			const msg = e instanceof Error ? e.message : String(e);
			log.error('schedule page load failed:', e);
			bannerMessage = msg;
		} finally {
			loading = false;
		}
	}

	onMount(loadVersion);

	$effect(() => {
		const snap = editableSnapshot();
		if (!editable) return;
		if (lastSavedSnapshot === null) return;
		if (snap === lastSavedSnapshot) return;
		lastSavedSnapshot = snap;
		scheduleSave();
	});

	function scheduleSave() {
		if (saveTimer) clearTimeout(saveTimer);
		saveStatus = 'saving';
		saveTimer = setTimeout(async () => {
			try {
				const id = page.params.id;
				// PARTIAL body on purpose — see the note at the top of this file.
				const res = await apiFetch(`/api/estimations/${id}/versions/draft`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						teamFte: currentTeamFte,
						dependencies: currentDependencies
					})
				});
				await assertOk(res, $_('editor.saveFailed'));
				saveStatus = 'saved';
				setTimeout(() => (saveStatus = 'idle'), 2000);
			} catch (e: unknown) {
				const msg = e instanceof Error ? e.message : String(e);
				saveStatus = 'idle';
				log.error('schedule autosave failed:', e);
				bannerMessage = $_('editor.autosaveFailed', { values: { message: msg } });
			}
		}, 800);
	}

	function days(v: number): string {
		return formatFixed(v, $locale ?? 'de', 1);
	}

	const backHref = $derived(
		resolve('/estimations/[id]/versions/[versionNumber]', {
			id: page.params.id!,
			versionNumber: page.params.versionNumber!
		}) + (page.url.searchParams.get('draft') === 'true' ? '?draft=true' : '')
	);
</script>

<Page width="full">
	<!-- The href IS resolve()d; the rule only models a bare resolve() call in the
	     attribute, not one concatenated with a query string. Same suppression the
	     AppHeader menu and the two computed-href call sites use — and the
	     `<a href=...` must stay on ONE line, or the disable lands on the wrong
	     line (noted in src/frontend/CLAUDE.md). -->
	<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
	<a href={backHref}
		class="text-sm text-brand-green hover:underline"
		data-testid="schedule-page-back">{$_('schedule.page.back')}</a
	>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	<h1 class="mt-4 mb-4 text-2xl font-bold" data-testid="schedule-page-title">
		{$_('schedule.page.title')}
	</h1>

	{#if loading}
		<p class="text-ink-muted">{$_('editor.loadingEditor')}</p>
	{:else}
		<div class="mb-4 flex flex-wrap items-end gap-6">
			<div>
				<label class="mb-1 block text-sm font-medium" for="team-fte-page"
					>{$_('schedule.teamSize.label')}</label
				>
				<input
					id="team-fte-page"
					type="number"
					min="1"
					step="1"
					class="w-56 rounded border border-hairline px-2 py-1 text-sm focus:border-brand-green focus:ring-1 focus:ring-brand-green/40 focus:outline-none"
					data-testid="team-fte"
					disabled={!editable}
					bind:value={currentTeamFte}
				/>
				<p class="mt-1 max-w-md text-xs text-ink-muted">{$_('schedule.teamSize.hint')}</p>
				{#if schedule?.error?.kind === 'INVALID_TEAM_FTE'}
					<p class="mt-1 text-xs text-red-700" data-testid="team-fte-error">
						{$_('schedule.error.invalidTeamFte')}
					</p>
				{/if}
			</div>

			{#if schedule && schedule.error == null}
				<div>
					<p class="text-xs text-ink-muted uppercase">{$_('schedule.plannedLength')}</p>
					<p class="text-lg font-semibold" data-testid="schedule-planned-length">
						{days(schedule.projectDurationDays)} {$_('schedule.days')}
					</p>
				</div>
				<div>
					<p class="text-xs text-ink-muted uppercase">{$_('schedule.uncertainty')}</p>
					<p class="text-lg font-semibold" data-testid="schedule-uncertainty">
						{days(schedule.optimisticDurationDays)}–{days(schedule.pessimisticDurationDays)}
						{$_('schedule.days')}
					</p>
				</div>
			{/if}

			{#if saveStatus !== 'idle'}
				<span class="text-xs text-ink-muted" data-testid="schedule-save-status"
					>{saveStatus === 'saving' ? $_('editor.saving') : $_('editor.saved')}</span
				>
			{/if}
		</div>

		<DependencyEditor
			{schedule}
			bind:dependencies={currentDependencies}
			{editable}
			{labels}
			{cycleCheck}
		/>

		<!-- The plan as bars, plus the Mermaid export (task-158). It lives HERE
		     and not in the version editor: task-167 gave this route the full
		     viewport because a plan needs the space, and task-170 removed the
		     per-item duration list from the editor on purpose. Available for
		     submitted versions too — exporting a snapshot's plan is the point. -->
		<section class="mt-6">
			<h2 class="mb-2 text-lg font-semibold">{$_('schedule.gantt.title')}</h2>
			<GanttChart {schedule} {variableItems} onerror={(m) => (bannerMessage = m)} />
		</section>
	{/if}
</Page>
