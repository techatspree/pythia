<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { onMount, type Component } from 'svelte';
	import { _ } from 'svelte-i18n';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import UndoHistoryPanel, { relativeTime } from '$lib/components/UndoHistoryPanel.svelte';
	import UndoConflictDialog from '$lib/components/UndoConflictDialog.svelte';
	import { computeCalcMap } from '$lib/adapter.js';
	import { normalizeRoots } from '$lib/estimationNodes';
	import { log } from '$lib/log';
	import type { ApiVersionResponse, ApiAdditionalCost } from '$lib/api/types.js';
	import type { components } from '$lib/api/schema';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
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

	const calcMap = $derived.by(() => {
		try {
			return computeCalcMap(
				currentRoots,
				{
					dailyRate: currentDailyRate,
					stdDevFactor: currentStdDevFactor,
					salesSurcharge: currentSalesSurcharge
				},
				currentDrivers,
				currentPhases
			);
		} catch (e: any) {
			log.error('calcMap computation failed:', e);
			bannerMessage = $_('editor.calculationFailed', { values: { message: e?.message ?? e } });
			return new Map<string, { offerPT: number; cost: number; offerPrice: number }>();
		}
	});

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
			roots: $state.snapshot(currentRoots)
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
						additionalCosts: currentAdditionalCosts
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
					<button
						onclick={submitVersion}
						class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>
						{$_('editor.submit')}
					</button>
				{/if}
				<details class="relative">
					<summary class="px-4 py-2 text-sm border rounded cursor-pointer select-none">{$_('editor.export')}</summary>
					<div class="absolute right-0 mt-1 bg-white border rounded shadow text-sm z-10">
						<a
							class="block px-4 py-2 hover:bg-gray-50"
							download
							rel="external"
							href="/api/estimations/{page.params.id}/versions/{versionData.isDraft ? 'draft' : versionData.versionNumber}/export?format=xlsx"
						>{$_('editor.exportXlsx')}</a>
						<a
							class="block px-4 py-2 hover:bg-gray-50"
							download
							rel="external"
							href="/api/estimations/{page.params.id}/versions/{versionData.isDraft ? 'draft' : versionData.versionNumber}/export?format=csv"
						>{$_('editor.exportCsv')}</a>
					</div>
				</details>
			</div>
		</div>

		{#if versionData.isDraft && showHistory}
			<UndoHistoryPanel history={undoStore.history} />
		{/if}

		<div class="flex items-center gap-3 mb-2">
			<h1 class="text-2xl font-bold">{$_('editor.versionHeading', { values: { n: versionData.versionNumber } })}</h1>
			<span
				data-testid="version-editor.method"
				class="px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green"
			>{formatMethodLabel(currentMethod)}</span>
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
