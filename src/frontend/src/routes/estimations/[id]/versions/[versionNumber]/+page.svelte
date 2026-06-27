<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import EstimationGrid from '$lib/components/EstimationGrid.svelte';
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import PhasesPanel from '$lib/components/PhasesPanel.svelte';
	import AdditionalCostsPanel from '$lib/components/AdditionalCostsPanel.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { computeCalcMap } from '$lib/adapter.js';
	import { log } from '$lib/log';
	import type { ApiVersionResponse, ApiAdditionalCost } from '$lib/api/types.js';

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
			const res = await fetch(url);
			if (!res.ok) throw new Error(`Failed to load version (${res.status})`);
			versionData = await res.json();
			currentNotes = versionData!.notes ?? '';
			currentRoots = versionData!.roots ?? [];
			currentParameters = versionData!.parameters ?? [];
			currentDrivers = versionData!.effortDrivers ?? [];
			currentPhases = versionData!.phases ?? [];
			currentAdditionalCosts = versionData!.additionalCosts ?? [];
		} catch (e: any) {
			bannerMessage = e.message;
			log.error('loadVersion failed:', e);
		} finally {
			loading = false;
		}
	}

	onMount(loadVersion);

	function scheduleSave() {
		if (saveTimer) clearTimeout(saveTimer);
		saveStatus = 'saving';
		saveTimer = setTimeout(async () => {
			try {
				const id = page.params.id;
				const res = await fetch(`/api/estimations/${id}/versions/draft`, {
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
				setTimeout(() => (saveStatus = 'idle'), 2000);
			} catch (e: any) {
				saveStatus = 'idle';
				bannerMessage = `Autosave failed: ${e?.message ?? e}`;
			}
		}, 800);
	}

	async function submitVersion() {
		const id = page.params.id!;
		const res = await fetch(`/api/estimations/${id}/versions/draft/submit`, {
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
				value={currentNotes}
				oninput={(e) => {
					currentNotes = e.currentTarget.value;
					scheduleSave();
				}}
			></textarea>
		{:else if versionData.notes}
			<p class="mb-4 text-sm text-gray-600 italic">{versionData.notes}</p>
		{/if}

		<ParametersPanel
			parameters={currentParameters}
			editable={versionData.isDraft}
			onchange={(params) => {
				currentParameters = params;
				scheduleSave();
			}}
		/>

		<EffortDriversPanel
			effortDrivers={currentDrivers}
			editable={versionData.isDraft}
			onchange={(drivers) => {
				currentDrivers = drivers;
				scheduleSave();
			}}
		/>

		<PhasesPanel
			phases={currentPhases}
			roots={currentRoots}
			{calcMap}
			editable={versionData.isDraft}
			onchange={(phases) => {
				currentPhases = phases;
				scheduleSave();
			}}
		/>

		<AdditionalCostsPanel
			costs={currentAdditionalCosts}
			phases={currentPhases}
			editable={versionData?.isDraft ?? false}
			onchange={(c) => {
				currentAdditionalCosts = c;
				scheduleSave();
			}}
		/>

		<EstimationGrid
			version={versionData}
			editable={versionData.isDraft}
			{calcMap}
			phases={currentPhases}
			onchange={(roots) => {
				currentRoots = roots;
				scheduleSave();
			}}
		/>
	{:else}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
	{/if}
</div>
