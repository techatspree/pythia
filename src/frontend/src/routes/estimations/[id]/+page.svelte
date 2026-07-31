<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { _ } from 'svelte-i18n';
	import EstimationDetail from '$lib/components/EstimationDetail.svelte';
	import VersionList from '$lib/components/VersionList.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import type { ApiEstimationDetail } from '$lib/api/types.js';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';

	let estimation = $state<ApiEstimationDetail | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	async function loadEstimation() {
		const id = page.params.id;
		loading = true;
		bannerMessage = null;
		try {
			const res = await apiFetch(`/api/estimations/${id}`);
			await assertOk(res, $_('estimation.pageNotFound'));
			estimation = await res.json();
		} catch (e: any) {
			log.error('loadEstimation failed:', e);
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	}

	onMount(loadEstimation);

	async function createVersion() {
		if (!estimation) return;
		try {
			const res = await apiFetch(`/api/estimations/${estimation.id ?? ''}/versions`, { method: 'POST' });
			await assertOk(res, $_('estimation.pageCreateVersionFailed'));
			await loadEstimation();
		} catch (e: any) {
			log.error('createVersion failed:', e);
			bannerMessage = e.message;
		}
	}

	// Launch a collaborative session pre-scoped to this offer. Route is resolved
	// via resolve(); the dynamic estimationId/projectId query is appended
	// separately because resolve() can only type-check a literal search suffix.
	const sessionHref = $derived(
		estimation?.id
			? `${resolve('/sessions')}?estimationId=${estimation.id}` +
				(estimation.projectId ? `&projectId=${estimation.projectId}` : '')
			: ''
	);

	async function submitVersion() {
		if (!estimation) return;
		try {
			const res = await apiFetch(`/api/estimations/${estimation.id ?? ''}/versions/draft/submit`, { method: 'POST' });
			await assertOk(res, $_('estimation.pageSubmitFailed'));
			await loadEstimation();
		} catch (e: any) {
			log.error('submitVersion failed:', e);
			bannerMessage = e.message;
		}
	}
</script>

<div class="p-6">
	{#if loading}
		<p class="text-gray-500">{$_('estimation.pageLoading')}</p>
	{:else}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		{#if estimation}
		{#if estimation.projectId}
			<a href={resolve('/projects/[id]', { id: estimation.projectId })} class="text-sm text-brand-green hover:underline mb-4 inline-block">{$_('estimation.pageBack', { values: { project: estimation.projectName ?? $_('estimation.pageProjectFallback') } })}</a>
		{/if}
		<div class="flex items-start justify-between gap-4">
			<div class="flex-1"><EstimationDetail {estimation} /></div>
			<!-- sessionHref resolves the route via resolve(); only the dynamic
			     estimationId/projectId query is concatenated, which this rule cannot model. -->
			<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
			<a href={sessionHref} class="shrink-0 mt-1 px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
				{$_('estimation.startSession')}
			</a>
		</div>
		<VersionList versions={estimation.versions} estimationId={estimation.id ?? ''} oncreate={createVersion} onsubmit={submitVersion} />
		{/if}
	{/if}
</div>
