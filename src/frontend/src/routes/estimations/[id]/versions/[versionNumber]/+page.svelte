<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { onMount, type Component } from 'svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import UndoHistoryPanel, { relativeTime } from '$lib/components/UndoHistoryPanel.svelte';
	import UndoConflictDialog from '$lib/components/UndoConflictDialog.svelte';
	import { computeCalcMap } from '$lib/adapter.js';
	import { normalizeRoots } from '$lib/estimationNodes';
	import { log } from '$lib/log';
	import type { ApiVersionResponse, ApiAdditionalCost } from '$lib/api/types.js';
	import type { components } from '$lib/api/schema';
	import { apiFetch } from '$lib/api/fetch';
	import { UndoStore } from '$lib/stores/undo.svelte';
	import { installUndoShortcuts } from '$lib/stores/undoKeyboard.svelte';
	import { loadEditorModule } from '$lib/methods/registry';

	type EstimationMethod = components['schemas']['EstimationMethod'];

	let versionData = $state<ApiVersionResponse | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);
	let saveStatus = $state<'idle' | 'saving' | 'saved'>('idle');
	let saveTimer: ReturnType<typeof setTimeout> | null = null;

	let currentNotes = $state('');
	let currentRoots = $state<any[]>([]);
	let currentParameters = $state<any[]>([]);
	let currentDrivers = $state<any[]>([]);
	let currentPhases = $state<any[]>([]);
	let currentAdditionalCosts = $state<ApiAdditionalCost[]>([]);

	// The estimation's method drives which editor module is lazy-loaded
	// (task-101). The version response carries no method, so loadVersion also
	// fetches the estimation to read it.
	let currentMethod = $state<EstimationMethod>('THREE_POINT_PERT');
	let EditorComponent = $state<Component<any> | null>(null);

	// Undo/redo plumbing (task-076). The store owns only the mutation log; this
	// page owns the draft state and applies the version undo/redo returns.
	const undoStore = new UndoStore(page.params.id!);
	undoStore.onResult = (version) => applyVersionData(version as unknown as ApiVersionResponse);

	// Undo/redo GUI (task-077).
	let showHistory = $state(false);
	// The history panel sits below the (potentially tall) grid, so scroll it
	// into view when opened — otherwise toggling it from the top toolbar looks
	// like nothing happened.
	let historyEl = $state<HTMLElement | null>(null);
	$effect(() => {
		if (showHistory && historyEl) {
			historyEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
		}
	});

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
			? `Rückgängig (Strg+Z) — ${undoTarget.kind} von ${undoTarget.userDisplayName}, ${relativeTime(undoTarget.createdAt)}`
			: 'Rückgängig (Strg+Z)'
	);
	const redoTooltip = $derived(
		redoTarget
			? `Wiederholen (Strg+Umschalt+Z) — ${redoTarget.kind} von ${redoTarget.userDisplayName}, ${relativeTime(redoTarget.createdAt)}`
			: 'Wiederholen (Strg+Umschalt+Z)'
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
			return computeCalcMap(currentRoots, currentParameters, currentDrivers, currentPhases);
		} catch (e: any) {
			log.error('calcMap computation failed:', e);
			bannerMessage = `Calculation failed: ${e?.message ?? e}`;
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
			if (!res.ok) throw new Error(`Failed to load version (${res.status})`);
			applyVersionData(await res.json());
			// Load the mutation log so undo/redo availability is known up front.
			if (versionData?.isDraft) await undoStore.refresh();
			// Read the estimation's method and lazy-load its editor module.
			const estRes = await apiFetch(`/api/estimations/${estimationId}`);
			if (estRes.ok) currentMethod = (await estRes.json()).method as EstimationMethod;
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
		currentParameters = (data.parameters ?? []).map((p: any) => ({
			name: p.name ?? '',
			value: p.value ?? 0,
			comment: p.comment ?? ''
		}));
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
			parameters: $state.snapshot(currentParameters),
			effortDrivers: $state.snapshot(currentDrivers),
			phases: $state.snapshot(currentPhases),
			additionalCosts: $state.snapshot(currentAdditionalCosts),
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
						roots: currentRoots,
						parameters: currentParameters,
						effortDrivers: currentDrivers,
						additionalCosts: currentAdditionalCosts
					})
				});
				if (!res.ok) throw new Error('Save failed');
				saveStatus = 'saved';
				// A successful PUT records a new mutation; refresh so undo/redo
				// availability tracks the latest state.
				await undoStore.refresh();
				setTimeout(() => (saveStatus = 'idle'), 2000);
			} catch (e: any) {
				saveStatus = 'idle';
				bannerMessage = `Autosave failed: ${e?.message ?? e}`;
			}
		}, 800);
	}

	async function submitVersion() {
		const id = page.params.id!;
		const res = await apiFetch(`/api/estimations/${id}/versions/draft/submit`, {
			method: 'POST'
		});
		if (res.ok) goto(resolve('/estimations/[id]', { id }));
	}
</script>

<div class="p-6">
	{#if loading}
		<p class="text-gray-500">Loading...</p>
	{:else if versionData}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		<div class="flex items-center justify-between mb-4">
			<a href={resolve('/estimations/[id]', { id: page.params.id! })} class="text-sm text-brand-green hover:underline"
				>&larr; Back to estimation</a
			>
			<div class="flex items-center gap-3">
				{#if saveStatus === 'saving'}
					<span class="text-sm text-gray-400">Saving…</span>
				{:else if saveStatus === 'saved'}
					<span class="text-sm text-green-600">Saved ✓</span>
				{/if}
				{#if versionData.isDraft}
					<button
						type="button"
						onclick={() => undoStore.undo()}
						disabled={!undoStore.canUndo}
						aria-label="Rückgängig"
						title={undoTooltip}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
					>
						↶ Rückgängig
					</button>
					<button
						type="button"
						onclick={() => undoStore.redo()}
						disabled={!undoStore.canRedo}
						aria-label="Wiederholen"
						title={redoTooltip}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
					>
						↷ Wiederholen
					</button>
					<button
						type="button"
						onclick={() => (showHistory = !showHistory)}
						aria-label="Verlauf anzeigen"
						aria-pressed={showHistory}
						class="px-3 py-2 text-sm border rounded hover:bg-gray-50 {showHistory
							? 'bg-brand-green/10 border-brand-green/40 text-brand-green'
							: ''}"
					>
						Verlauf
					</button>
					<button
						onclick={submitVersion}
						class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>
						Submit
					</button>
				{/if}
				<details class="relative">
					<summary class="px-4 py-2 text-sm border rounded cursor-pointer select-none">Export</summary>
					<div class="absolute right-0 mt-1 bg-white border rounded shadow text-sm z-10">
						<a
							class="block px-4 py-2 hover:bg-gray-50"
							download
							rel="external"
							href="/api/estimations/{page.params.id}/versions/{versionData.isDraft ? 'draft' : versionData.versionNumber}/export?format=xlsx"
						>Excel (.xlsx)</a>
						<a
							class="block px-4 py-2 hover:bg-gray-50"
							download
							rel="external"
							href="/api/estimations/{page.params.id}/versions/{versionData.isDraft ? 'draft' : versionData.versionNumber}/export?format=csv"
						>CSV (.csv)</a>
					</div>
				</details>
			</div>
		</div>

		<h1 class="text-2xl font-bold mb-2">Version {versionData.versionNumber}</h1>

		{#if !versionData.isDraft}
			<div
				class="mb-4 px-4 py-3 bg-amber-50 border border-amber-200 rounded text-amber-800 text-sm"
			>
				Submitted — read only
			</div>
		{/if}

		{#if versionData.isDraft}
			<textarea
				class="w-full mb-4 p-2 border rounded text-sm resize-none focus:outline-none focus:ring-1 focus:ring-brand-green/40"
				rows="2"
				placeholder="Notes…"
				bind:value={currentNotes}
			></textarea>
		{:else if versionData.notes}
			<p class="mb-4 text-sm text-gray-600 italic">{versionData.notes}</p>
		{/if}

		{#if EditorComponent}
			<EditorComponent
				bind:roots={currentRoots}
				bind:parameters={currentParameters}
				bind:effortDrivers={currentDrivers}
				bind:phases={currentPhases}
				bind:additionalCosts={currentAdditionalCosts}
				{calcMap}
				editable={versionData.isDraft}
			/>
		{:else}
			<p class="text-gray-500">Loading editor…</p>
		{/if}

		{#if versionData.isDraft && showHistory}
			<div bind:this={historyEl} class="scroll-mt-4">
				<UndoHistoryPanel history={undoStore.history} />
			</div>
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
