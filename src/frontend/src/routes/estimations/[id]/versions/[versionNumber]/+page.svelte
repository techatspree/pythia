<script lang="ts">
	import Badge from '$lib/ui/Badge.svelte';
	import Button from '$lib/ui/Button.svelte';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { onMount, type Component } from 'svelte';
	import { _, locale } from 'svelte-i18n';
	import { formatFixed } from '$lib/format';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import UndoHistoryPanel, { relativeTime } from '$lib/components/UndoHistoryPanel.svelte';
	import UndoConflictDialog from '$lib/components/UndoConflictDialog.svelte';
	import { computeEstimation, ZERO_TOTALS } from '$lib/adapter.js';
	import type { ScheduleEdge } from '$lib/adapter.js';
	import EstimationSummaryPanel from '$lib/components/EstimationSummaryPanel.svelte';
	import { normalizeRoots, type CalcEntry } from '$lib/estimationNodes';
	import { log } from '$lib/log';
	import type { ApiVersionResponse, ApiAdditionalCost } from '$lib/api/types.js';
	import type { components } from '$lib/api/schema';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { downloadResponse } from '$lib/api/download';
	import { UndoStore } from '$lib/stores/undo.svelte';
	import { installUndoShortcuts } from '$lib/stores/undoKeyboard.svelte';
	import { loadEditorModule } from '$lib/methods/registry';
	import { formatMethodLabel } from '$lib/methods/labels';

	type EstimationMethod = components['schemas']['EstimationMethod'];
	// Bucket + sampled method (task-104). Read untyped from the estimation
	// detail; ids are client-assigned so leaves reference them by id.
	type Bucket = { id: string; position: number; label: string };

	let versionData = $state<ApiVersionResponse | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);
	let saveStatus = $state<'idle' | 'saving' | 'saved'>('idle');
	let saveTimer: ReturnType<typeof setTimeout> | null = null;

	let currentNotes = $state('');
	let currentRoots = $state<any[]>([]);
	// Typed calculation inputs (task-138) — no longer a renameable name/value list.
	let currentDailyRate = $state(800);
	let currentStdDevFactor = $state(2.0);
	let currentSalesSurcharge = $state(0.1);
	let currentDrivers = $state<any[]>([]);
	let currentPhases = $state<any[]>([]);
	let currentAdditionalCosts = $state<ApiAdditionalCost[]>([]);
	// Schedule inputs (task-156's wire shape). teamFte is a WORKER COUNT since
	// task-166, not a divisor on effort.
	let currentTeamFte = $state(1);
	let currentDependencies = $state<ScheduleEdge[]>([]);
	let scheduleOpen = $state(false);
	// Buckets of the bucket+sampled method; empty for PERT estimations (task-104).
	let currentBuckets = $state<Bucket[]>([]);

	// The estimation's method drives which editor module is lazy-loaded
	// (task-101). The version response carries no method, so loadVersion also
	// fetches the estimation to read it.
	let currentMethod = $state<EstimationMethod>('THREE_POINT_PERT');
	let EditorComponent = $state<Component<any> | null>(null);

	// Undo/redo plumbing (task-076). The store owns only the mutation log; this
	// page owns the draft state and applies the version undo/redo returns.
	const undoStore = new UndoStore(page.params.id!);
	undoStore.onResult = (version) => applyVersionData(version as unknown as ApiVersionResponse);

	// Undo/redo GUI (task-077). The history section renders directly under the
	// toolbar (task-109), so toggling it is visible where the button is and no
	// scroll-into-view workaround is needed.
	let showHistory = $state(false);

	// Export (xlsx/csv). It must NOT be a plain `<a href="/api/…" download>`:
	// browser navigation sends no `Authorization` header, so the backend answers
	// `401` and the browser saves that JSON error body as the export file. The
	// request goes through `apiFetch` and the body is downloaded as a blob.
	let exportMenuOpen = $state(false);
	let exporting = $state(false);

	async function exportVersion(format: 'xlsx' | 'csv') {
		if (!versionData) return;
		const versionRef = versionData.isDraft ? 'draft' : String(versionData.versionNumber);
		exportMenuOpen = false;
		exporting = true;
		bannerMessage = null;
		try {
			const res = await apiFetch(
				`/api/estimations/${page.params.id}/versions/${versionRef}/export?format=${format}`
			);
			await assertOk(res, $_('editor.exportFailed'));
			downloadResponse(
				await res.blob(),
				res.headers.get('Content-Disposition'),
				`estimation-${versionRef}.${format}`
			);
		} catch (e: any) {
			log.error('exportVersion failed:', e);
			bannerMessage = e.message;
		} finally {
			exporting = false;
		}
	}

	// The entry a given action would target, for the toolbar tooltips.
	function latestByStatus(status: string) {
		const matches = undoStore.history.filter((e) => e.status === status);
		return matches.length
			? matches.reduce((a, b) => (a.sequenceNumber > b.sequenceNumber ? a : b))
			: null;
	}
	const undoTarget = $derived(latestByStatus('ACTIVE'));
	const redoTarget = $derived(latestByStatus('UNDONE'));
	const undoTooltip = $derived(
		undoTarget
			? $_('editor.undoTooltipDetail', {
					values: {
						kind: undoTarget.kind,
						user: undoTarget.userDisplayName,
						time: relativeTime(undoTarget.createdAt)
					}
				})
			: $_('editor.undoTooltip')
	);
	const redoTooltip = $derived(
		redoTarget
			? $_('editor.redoTooltipDetail', {
					values: {
						kind: redoTarget.kind,
						user: redoTarget.userDisplayName,
						time: relativeTime(redoTarget.createdAt)
					}
				})
			: $_('editor.redoTooltip')
	);

	// Recommended conflict resolution: reload the current draft, refresh the log,
	// then dismiss the dialog.
	async function reloadAfterConflict() {
		await loadVersion();
		await undoStore.refresh();
		undoStore.clearConflict();
	}

	// One domain round-trip per edit: the version is built and calculated once,
	// and both the per-node calc map and the whole-estimation totals are read off
	// that single result.
	const estimation = $derived.by(() => {
		try {
			return computeEstimation(
				currentRoots,
				{
					dailyRate: currentDailyRate,
					stdDevFactor: currentStdDevFactor,
					salesSurcharge: currentSalesSurcharge
				},
				currentDrivers,
				currentPhases,
				currentAdditionalCosts,
				// Same round-trip: the schedule is read off the version this call
				// already builds and calculates.
				{ dependencies: currentDependencies, teamFte: currentTeamFte }
			);
		} catch (e: any) {
			log.error('estimation computation failed:', e);
			bannerMessage = $_('editor.calculationFailed', { values: { message: e?.message ?? e } });
			return { calcMap: new Map<string, CalcEntry>(), totals: ZERO_TOTALS, schedule: null };
		}
	});
	const calcMap = $derived(estimation.calcMap);
	const totals = $derived(estimation.totals);
	const schedule = $derived(estimation.schedule);

	async function loadVersion() {
		loading = true;
		bannerMessage = null;
		try {
			const estimationId = page.params.id;
			const versionNumber = page.params.versionNumber;
			const isDraft = page.url.searchParams.get('draft') === 'true';
			const url = isDraft
				? `/api/estimations/${estimationId}/versions/draft`
				: `/api/estimations/${estimationId}/versions/${versionNumber}`;
			const res = await apiFetch(url);
			await assertOk(res, $_('editor.loadFailed'));
			const data = await res.json();
			// Read the estimation detail first: it carries the method (which module
			// to load) and, for bucket estimations, the buckets. Setting
			// currentBuckets BEFORE applyVersionData folds them into the autosave
			// baseline, so loading buckets never triggers a spurious PUT.
			const estRes = await apiFetch(`/api/estimations/${estimationId}`);
			if (estRes.ok) {
				const est = await estRes.json();
				currentMethod = (est.method ?? currentMethod) as EstimationMethod;
				currentBuckets = ((est.buckets ?? []) as Bucket[])
					.map((b) => ({ id: b.id, position: b.position ?? 0, label: b.label ?? '' }))
					.sort((a, b) => a.position - b.position);
			}
			applyVersionData(data);
			// Fresh bucket draft: seed default sizes so the estimator can assign
			// items right away. AFTER the baseline, so the autosave effect persists
			// them (new client-assigned ids).
			if (data.isDraft && currentMethod === 'BUCKET_SAMPLED_PERT' && currentBuckets.length === 0) {
				currentBuckets = ['XS', 'S', 'M', 'L', 'XL'].map((label, i) => ({
					id: crypto.randomUUID(),
					position: i,
					label
				}));
			}
			// Load the mutation log so undo/redo availability is known up front.
			if (versionData?.isDraft) await undoStore.refresh();
			EditorComponent = (await loadEditorModule(currentMethod)).default;
		} catch (e: any) {
			bannerMessage = e.message;
			log.error('loadVersion failed:', e);
		} finally {
			loading = false;
		}
	}

	// Normalise a version response into the page's editable `$state` and reset
	// the autosave baseline. Shared by the initial load and the undo/redo path
	// (single source for the mapping, so undo re-hydrates exactly like a load).
	function applyVersionData(data: ApiVersionResponse) {
		versionData = data;
		currentNotes = data.notes ?? '';
		currentRoots = normalizeRoots(data);
		currentDailyRate = data.dailyRate ?? 800;
		currentStdDevFactor = data.stdDevFactor ?? 2.0;
		currentSalesSurcharge = data.salesSurcharge ?? 0.1;
		currentDrivers = (data.effortDrivers ?? []).map((d: any) => ({
			description: d.description ?? '',
			factor: d.factor ?? 0,
			comment: d.comment ?? ''
		}));
		currentPhases = (data.phases ?? []).map((p: any) => ({
			name: p.name ?? '',
			abbreviation: p.abbreviation ?? '',
			durationWeeks: p.durationWeeks ?? null
		}));
		currentAdditionalCosts = (data.additionalCosts ?? []).map((c: any) => ({
			id: c.id ?? null,
			description: c.description ?? '',
			amount: c.amount ?? 0,
			type: c.type,
			amountPerWeek: c.amountPerWeek ?? null,
			phaseAbbreviation: c.phaseAbbreviation ?? null
		}));
		// Hydrated here, like every other editable field, so an undo or reload
		// does not resurrect stale values or trigger a spurious autosave.
		currentTeamFte = (data as any).teamFte ?? 1;
		currentDependencies = ((data as any).dependencies ?? []).map((d: any) => ({
			fromLogicalId: d.fromLogicalId,
			toLogicalId: d.toLogicalId
		}));
		// Baseline for the autosave effect: any subsequent change to the
		// editable state (and only those) triggers a save.
		lastSavedSnapshot = editableSnapshot();
	}

	onMount(loadVersion);

	// Install/remove the global undo/redo keyboard shortcuts with the component.
	$effect(() => installUndoShortcuts(undoStore));

	// Surface non-409 undo/redo failures through the existing ErrorBanner.
	$effect(() => {
		const err = undoStore.error;
		if (err) {
			bannerMessage = err;
			undoStore.error = null;
		}
	});

	// Non-reactive baseline; null until the first successful load.
	let lastSavedSnapshot: string | null = null;

	function editableSnapshot(): string {
		return JSON.stringify({
			notes: currentNotes,
			dailyRate: currentDailyRate,
			stdDevFactor: currentStdDevFactor,
			salesSurcharge: currentSalesSurcharge,
			effortDrivers: $state.snapshot(currentDrivers),
			phases: $state.snapshot(currentPhases),
			additionalCosts: $state.snapshot(currentAdditionalCosts),
			buckets: $state.snapshot(currentBuckets),
			roots: $state.snapshot(currentRoots),
			// `dependencies` is deliberately ABSENT: the schedule route owns
			// editing them (task-167), so including them here would let a stale
			// copy overwrite a graph edit on the next unrelated save. They are
			// still loaded, because the numbers above are computed from them.
			teamFte: currentTeamFte
		});
	}

	// Single reactive autosave: fires only on an actual edit to a draft.
	// The deep-read snapshot tracks nested mutations; the baseline comparison
	// guarantees no PUT happens on load (or reload).
	$effect(() => {
		const snap = editableSnapshot();
		if (!versionData?.isDraft) return;
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
				const res = await apiFetch(`/api/estimations/${id}/versions/draft`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						notes: currentNotes,
						phases: currentPhases,
						buckets: currentBuckets,
						roots: currentRoots,
						dailyRate: currentDailyRate,
						stdDevFactor: currentStdDevFactor,
						salesSurcharge: currentSalesSurcharge,
						effortDrivers: currentDrivers,
						additionalCosts: currentAdditionalCosts,
						// No `dependencies`: a null field means "leave unchanged"
						// (task-156), so omitting it preserves whatever the schedule
						// route saved. teamFte stays — it is editable in both places.
						teamFte: currentTeamFte
					})
				});
				await assertOk(res, $_('editor.saveFailed'));
				saveStatus = 'saved';
				// A successful PUT records a new mutation; refresh so undo/redo
				// availability tracks the latest state.
				await undoStore.refresh();
				setTimeout(() => (saveStatus = 'idle'), 2000);
			} catch (e: any) {
				saveStatus = 'idle';
				bannerMessage = $_('editor.autosaveFailed', { values: { message: e?.message ?? e } });
			}
		}, 800);
	}

	async function submitVersion() {
		const id = page.params.id!;
		try {
			const res = await apiFetch(`/api/estimations/${id}/versions/draft/submit`, {
				method: 'POST'
			});
			await assertOk(res, $_('editor.submitFailed'));
			goto(resolve('/estimations/[id]', { id }));
		} catch (e: any) {
			log.error('submitVersion failed:', e);
			bannerMessage = e.message;
		}
	}
</script>

<div class="p-6">
	{#if loading}
		<p class="text-gray-500">{$_('editor.loading')}</p>
	{:else if versionData}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		<div class="flex items-center justify-between mb-4">
			<a href={resolve('/estimations/[id]', { id: page.params.id! })} class="text-sm text-brand-green hover:underline"
				>{$_('editor.back')}</a
			>
			<div class="flex items-center gap-3">
				{#if saveStatus === 'saving'}
					<span class="text-sm text-gray-400">{$_('editor.saving')}</span>
				{:else if saveStatus === 'saved'}
					<span class="text-sm text-green-600">{$_('editor.saved')}</span>
				{/if}
				{#if versionData.isDraft}
					<button
						type="button"
						onclick={() => undoStore.undo()}
						disabled={!undoStore.canUndo}
						aria-label={$_('editor.undo')}
						title={undoTooltip}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
					>
						{$_('editor.undoLabel')}
					</button>
					<button
						type="button"
						onclick={() => undoStore.redo()}
						disabled={!undoStore.canRedo}
						aria-label={$_('editor.redo')}
						title={redoTooltip}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
					>
						{$_('editor.redoLabel')}
					</button>
					<button
						type="button"
						onclick={() => (showHistory = !showHistory)}
						aria-label={$_('editor.historyAria')}
						aria-pressed={showHistory}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 {showHistory
							? 'bg-brand-green/10 border-brand-green/40 text-brand-green'
							: ''}"
					>
						{$_('editor.history')}
					</button>
					<Button
						onclick={submitVersion}
					
					>
						{$_('editor.submit')}
					</Button>
				{/if}
				<details class="relative" bind:open={exportMenuOpen}>
					<summary class="px-4 py-2 text-sm border rounded cursor-pointer select-none">{$_('editor.export')}</summary>
					<div class="absolute right-0 mt-1 bg-white border rounded shadow text-sm z-10">
						<button
							type="button"
							class="block w-full text-left px-4 py-2 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
							disabled={exporting}
							onclick={() => exportVersion('xlsx')}
						>{$_('editor.exportXlsx')}</button>
						<button
							type="button"
							class="block w-full text-left px-4 py-2 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
							disabled={exporting}
							onclick={() => exportVersion('csv')}
						>{$_('editor.exportCsv')}</button>
					</div>
				</details>
			</div>
		</div>

		{#if versionData.isDraft && showHistory}
			<UndoHistoryPanel history={undoStore.history} />
		{/if}

		<div class="flex items-center gap-3 mb-2">
			<h1 class="text-2xl font-bold">{$_('editor.versionHeading', { values: { n: versionData.versionNumber } })}</h1>
			<Badge
				data-testid="version-editor.method"
				variant="brand"
			>{formatMethodLabel(currentMethod)}</Badge>
		</div>

		{#if !versionData.isDraft}
			<div
				class="mb-4 px-4 py-3 bg-amber-50 border border-amber-200 rounded text-amber-800 text-sm"
			>
				{$_('editor.submittedReadOnly')}
			</div>
		{/if}

		{#if versionData.isDraft}
			<textarea
				class="w-full mb-4 p-2 border rounded text-sm resize-none focus:outline-none focus:ring-1 focus:ring-brand-green/40"
				rows="2"
				placeholder={$_('editor.notesPlaceholder')}
				bind:value={currentNotes}
			></textarea>
		{:else if versionData.notes}
			<p class="mb-4 text-sm text-gray-600 italic">{versionData.notes}</p>
		{/if}

		<EstimationSummaryPanel {totals} />

		<!-- Schedule (task-157). The two figures below are DIFFERENT
		     measurements and are deliberately not merged into one range: the
		     planned length is the resource-levelled makespan, while the band is
		     estimate uncertainty along the critical chain with capacity ignored
		     (task-166). At one worker with three independent 10-day items the
		     makespan is 30 while the band is [10, 10] — outside it entirely. -->
		<section class="mb-4 rounded-lg border">
			<button
				type="button"
				class="flex w-full items-center justify-between bg-brand-green/10 px-4 py-2 text-left text-xs font-semibold tracking-wide text-brand-green uppercase"
				data-testid="schedule-section-toggle"
				onclick={() => (scheduleOpen = !scheduleOpen)}
			>
				<span>{$_('schedule.title')}</span>
				<span aria-hidden="true">{scheduleOpen ? '▾' : '▸'}</span>
			</button>
			{#if scheduleOpen}
				<div class="p-4">
					<div class="mb-4 max-w-md">
						<label class="mb-1 block text-sm font-medium" for="team-fte"
							>{$_('schedule.teamSize.label')}</label
						>
						<input
							id="team-fte"
							type="number"
							min="1"
							step="1"
							class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-brand-green focus:ring-1 focus:ring-brand-green/40 focus:outline-none"
							data-testid="team-fte"
							disabled={!versionData.isDraft}
							bind:value={currentTeamFte}
						/>
						<p class="mt-1 text-xs text-gray-500">{$_('schedule.teamSize.hint')}</p>
						{#if schedule?.error?.kind === 'INVALID_TEAM_FTE'}
							<p class="mt-1 text-xs text-red-700" data-testid="team-fte-error">
								{$_('schedule.error.invalidTeamFte')}
							</p>
						{/if}
					</div>

					{#if schedule && schedule.error == null}
						<div class="mb-4 grid gap-4 sm:grid-cols-2">
							<div>
								<p class="text-xs text-gray-500 uppercase">{$_('schedule.plannedLength')}</p>
								<p class="text-lg font-semibold" data-testid="schedule-planned-length">
									{formatFixed(schedule.projectDurationDays, $locale ?? 'de', 1)}
									{$_('schedule.days')}
								</p>
								<p class="text-xs text-gray-500">{$_('schedule.plannedLengthHint')}</p>
							</div>
							<div>
								<p class="text-xs text-gray-500 uppercase">{$_('schedule.uncertainty')}</p>
								<p class="text-lg font-semibold" data-testid="schedule-uncertainty">
									{formatFixed(schedule.optimisticDurationDays, $locale ?? 'de', 1)}–{formatFixed(
										schedule.pessimisticDurationDays,
										$locale ?? 'de',
										1
									)}
									{$_('schedule.days')}
								</p>
								<p class="text-xs text-gray-500">{$_('schedule.uncertaintyHint')}</p>
							</div>
						</div>
					{/if}

					<!-- The graph lives on its own route (task-167): a net plan needs
					     the full viewport, and this section keeps only the numbers. -->
					<Button
						href={resolve('/estimations/[id]/versions/[versionNumber]/schedule', {
							id: page.params.id!,
							versionNumber: page.params.versionNumber!
						}) + (versionData.isDraft ? '?draft=true' : '')}
						variant="secondary"
						size="sm"
					>
						{$_('schedule.page.open')}
					</Button>

					{#if schedule && schedule.error == null && schedule.tasks.length > 0}
						<table class="mt-4 w-full text-sm" data-testid="schedule-durations">
							<thead>
								<tr class="border-b text-left text-xs text-gray-500 uppercase">
									<th class="py-1">{$_('schedule.perNode')}</th>
									<th class="py-1 text-right">{$_('schedule.days')}</th>
									<th class="py-1 text-right">{$_('schedule.criticalChain')}</th>
								</tr>
							</thead>
							<tbody>
								{#each schedule.tasks as task (task.logicalId)}
									<tr class="border-b border-gray-100">
										<td class="py-1" style="padding-left: {task.depth * 16}px">{task.title}</td>
										<td class="py-1 text-right"
											>{formatFixed(task.durationDays, $locale ?? 'de', 1)}</td
										>
										<td class="py-1 text-right">{task.onCriticalPath ? '●' : ''}</td>
									</tr>
								{/each}
							</tbody>
						</table>
					{/if}
				</div>
			{/if}
		</section>

		{#if EditorComponent}
			<EditorComponent
				bind:roots={currentRoots}
				bind:dailyRate={currentDailyRate}
				bind:stdDevFactor={currentStdDevFactor}
				bind:salesSurcharge={currentSalesSurcharge}
				bind:effortDrivers={currentDrivers}
				bind:phases={currentPhases}
				bind:additionalCosts={currentAdditionalCosts}
				bind:buckets={currentBuckets}
				{calcMap}
				{totals}
				editable={versionData.isDraft}
			/>
		{:else}
			<p class="text-gray-500">{$_('editor.loadingEditor')}</p>
		{/if}

		{#if undoStore.conflict}
			<UndoConflictDialog
				conflict={undoStore.conflict!}
				onreload={reloadAfterConflict}
				oncancel={() => undoStore.clearConflict()}
			/>
		{/if}
	{:else}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
	{/if}
</div>
