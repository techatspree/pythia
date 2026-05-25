<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import EstimationDetail from '$lib/components/EstimationDetail.svelte';
	import VersionList from '$lib/components/VersionList.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import type { ApiEstimationDetail } from '$lib/api/types.js';

	let estimation = $state<ApiEstimationDetail | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	async function loadEstimation() {
		const id = page.params.id;
		loading = true;
		bannerMessage = null;
		try {
			const res = await fetch(`/api/estimations/${id}`);
			if (!res.ok) throw new Error('Estimation not found');
			estimation = await res.json();
		} catch (e: any) {
			bannerMessage = e.message;
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
			bannerMessage = e.message;
		}
	}

	async function submitVersion() {
		if (!estimation) return;
		try {
			const res = await fetch(`/api/estimations/${estimation.id ?? ''}/versions/draft/submit`, { method: 'POST' });
			if (!res.ok) throw new Error('Failed to submit version');
			await loadEstimation();
		} catch (e: any) {
			bannerMessage = e.message;
		}
	}
</script>

<div class="max-w-5xl mx-auto p-6">
	{#if loading}
		<p class="text-gray-500">Loading estimation...</p>
	{:else}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		{#if estimation}
		{#if estimation.projectId}
			<a href="/projects/{estimation.projectId}" class="text-sm text-brand-green hover:underline mb-4 inline-block">&larr; Back to {estimation.projectName ?? 'project'}</a>
		{/if}
		<EstimationDetail {estimation} />
		<VersionList versions={estimation.versions} estimationId={estimation.id ?? ''} oncreate={createVersion} onsubmit={submitVersion} />
		{/if}
	{/if}
</div>
