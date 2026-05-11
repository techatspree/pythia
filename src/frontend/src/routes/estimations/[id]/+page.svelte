<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import EstimationDetail from '$lib/components/EstimationDetail.svelte';
	import VersionList from '$lib/components/VersionList.svelte';
	import type { ApiEstimationDetail } from '$lib/api/types.js';

	let estimation = $state<ApiEstimationDetail | null>(null);
	let loading = $state(true);
	let error = $state('');

	async function loadEstimation() {
		const id = page.params.id;
		loading = true;
		error = '';
		try {
			const res = await fetch(`/api/estimations/${id}`);
			if (!res.ok) throw new Error('Estimation not found');
			estimation = await res.json();
		} catch (e: any) {
			error = e.message;
		} finally {
			loading = false;
		}
	}

	onMount(loadEstimation);

	async function createVersion() {
		if (!estimation) return;
		try {
			const res = await fetch(`/api/estimations/${estimation.id ?? ''}/versions`, { method: 'POST' });
			if (!res.ok) throw new Error('Failed to create version');
			await loadEstimation();
		} catch (e: any) {
			error = e.message;
		}
	}

	async function submitVersion() {
		if (!estimation) return;
		try {
			const res = await fetch(`/api/estimations/${estimation.id ?? ''}/versions/draft/submit`, { method: 'POST' });
			if (!res.ok) throw new Error('Failed to submit version');
			await loadEstimation();
		} catch (e: any) {
			error = e.message;
		}
	}
</script>

<div class="max-w-5xl mx-auto p-6">
	{#if loading}
		<p class="text-gray-500">Loading estimation...</p>
	{:else if error}
		<p class="text-red-600">{error}</p>
	{:else if estimation}
		{#if estimation.projectId}
			<a href="/projects/{estimation.projectId}" class="text-sm text-brand-green hover:underline mb-4 inline-block">&larr; Back to {estimation.projectName ?? 'project'}</a>
		{/if}
		<EstimationDetail {estimation} />
		<VersionList versions={estimation.versions} estimationId={estimation.id ?? ''} oncreate={createVersion} onsubmit={submitVersion} />
	{/if}
</div>
