<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import EstimationGrid from '$lib/components/EstimationGrid.svelte';
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import { computeCalcMap } from '$lib/domain/adapter.js';
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

	const calcMap = $derived(computeCalcMap(currentGroups, currentParameters, currentDrivers));

	const estimationId = page.params.id;
	const versionNumber = page.params.versionNumber;
	const isDraft = page.url.searchParams.get('draft') === 'true';

	async function loadVersion() {
		loading = true;
		error = '';
		try {
			const url = isDraft
				? `/api/estimations/${estimationId}/versions/draft`
				: `/api/estimations/${estimationId}/versions/${versionNumber}`;
			const res = await fetch(url);
			if (!res.ok) throw new Error('Failed to load version');
			versionData = await res.json();
			currentNotes = versionData!.notes ?? '';
			currentGroups = versionData!.itemGroups ?? [];
			currentParameters = versionData!.parameters ?? [];
			currentDrivers = versionData!.effortDrivers ?? [];
		} catch (e: any) {
			error = e.message;
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
				const res = await fetch(`/api/estimations/${estimationId}/versions/draft`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						notes: currentNotes,
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
		const res = await fetch(`/api/estimations/${estimationId}/versions/draft/submit`, {
			method: 'POST'
		});
		if (res.ok) goto(`/estimations/${estimationId}`);
	}
</script>

<div class="max-w-6xl mx-auto p-6">
	{#if loading}
		<p class="text-gray-500">Loading...</p>
	{:else if error}
		<p class="text-red-600">{error}</p>
	{:else if versionData}
		<div class="flex items-center justify-between mb-4">
			<a href="/estimations/{estimationId}" class="text-sm text-brand-green hover:underline"
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

		<EstimationGrid
			version={versionData}
			editable={versionData.isDraft}
			{calcMap}
			onchange={(groups) => {
				currentGroups = groups;
				scheduleSave();
			}}
		/>
	{/if}
</div>
