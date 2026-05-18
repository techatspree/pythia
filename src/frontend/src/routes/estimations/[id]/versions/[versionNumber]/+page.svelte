<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import EstimationGrid from '$lib/components/EstimationGrid.svelte';
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import PhasesPanel from '$lib/components/PhasesPanel.svelte';
	import { computeCalcMap } from '$lib/adapter.js';
	import type { ApiVersionResponse } from '$lib/api/types.js';

	let versionData = $state<ApiVersionResponse | null>(null);
	let loading = $state(true);
	let error = $state('');
	let saveStatus = $state<'idle' | 'saving' | 'saved'>('idle');
	let saveTimer: ReturnType<typeof setTimeout> | null = null;

	let currentNotes = $state('');
	let currentGroups = $state<any[]>([]);
	let currentParameters = $state<any[]>([]);
	let currentDrivers = $state<any[]>([]);
	let currentPhases = $state<any[]>([]);

	const calcMap = $derived.by(() => {
		try {
			return computeCalcMap(currentGroups, currentParameters, currentDrivers, currentPhases);
		} catch (e) {
			console.error('calcMap computation failed:', e);
			return new Map<string, { offerPT: number; cost: number; offerPrice: number }>();
		}
	});

	async function loadVersion() {
		loading = true;
		error = '';
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
			currentGroups = versionData!.itemGroups ?? [];
			currentParameters = versionData!.parameters ?? [];
			currentDrivers = versionData!.effortDrivers ?? [];
			currentPhases = versionData!.phases ?? [];
		} catch (e: any) {
			error = e.message;
			console.error('loadVersion failed:', e);
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
						itemGroups: currentGroups,
						parameters: currentParameters,
						effortDrivers: currentDrivers
					})
				});
				if (!res.ok) throw new Error('Save failed');
				saveStatus = 'saved';
				setTimeout(() => (saveStatus = 'idle'), 2000);
			} catch {
				saveStatus = 'idle';
			}
		}, 800);
	}

	async function submitVersion() {
		const id = page.params.id;
		const res = await fetch(`/api/estimations/${id}/versions/draft/submit`, {
			method: 'POST'
		});
		if (res.ok) goto(`/estimations/${id}`);
	}
</script>

<div class="max-w-6xl mx-auto p-6">
	{#if loading}
		<p class="text-gray-500">Loading...</p>
	{:else if error}
		<p class="text-red-600">{error}</p>
	{:else if versionData}
		<div class="flex items-center justify-between mb-4">
			<a href="/estimations/{page.params.id}" class="text-sm text-brand-green hover:underline"
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
							href="/api/estimations/{page.params.id}/versions/{versionData.isDraft ? 'draft' : versionData.versionNumber}/export?format=xlsx"
						>Excel (.xlsx)</a>
						<a
							class="block px-4 py-2 hover:bg-gray-50"
							download
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
			itemGroups={currentGroups}
			{calcMap}
			editable={versionData.isDraft}
			onchange={(phases) => {
				currentPhases = phases;
				scheduleSave();
			}}
		/>

		<EstimationGrid
			version={versionData}
			editable={versionData.isDraft}
			{calcMap}
			phases={currentPhases}
			onchange={(groups) => {
				currentGroups = groups;
				scheduleSave();
			}}
		/>
	{/if}
</div>
