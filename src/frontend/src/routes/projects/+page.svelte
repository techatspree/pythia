<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import { untrack } from 'svelte';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { _ } from 'svelte-i18n';
	import ProjectList from '$lib/components/ProjectList.svelte';
	import CreateProjectDialog from '$lib/components/CreateProjectDialog.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';

	let projects = $state<any[]>([]);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);
	let showCreate = $state(false);
	let search = $state('');

	// "Only my projects" is on by default (task-200); turning it off is kept in
	// the URL as `?scope=all`, so a reload or a shared link keeps the full list.
	let onlyMine = $derived(page.url.searchParams.get('scope') !== 'all');

	function setOnlyMine(on: boolean) {
		const url = new URL(page.url);
		if (on) url.searchParams.delete('scope');
		else url.searchParams.set('scope', 'all');
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- same route, only the query changes
		goto(url, { replaceState: true, keepFocus: true, noScroll: true });
	}

	// Toggling quickly can leave two loads in flight; only the latest may land.
	let loadSeq = 0;

	async function loadProjects() {
		const seq = ++loadSeq;
		loading = true;
		bannerMessage = null;
		try {
			const res = await apiFetch(onlyMine ? '/api/projects?owner=me' : '/api/projects');
			await assertOk(res, $_('project.pageLoadFailed'));
			const body = await res.json();
			if (seq === loadSeq) projects = body;
		} catch (e: any) {
			log.error('loadProjects failed:', e);
			bannerMessage = e.message;
		} finally {
			if (seq === loadSeq) loading = false;
		}
	}

	// Re-runs whenever the filter (i.e. the URL) changes.
	$effect(() => {
		void onlyMine;
		untrack(loadProjects);
	});

	let filteredProjects = $derived(
		search.trim()
			? projects.filter(p =>
				p.name?.toLowerCase().includes(search.toLowerCase()) ||
				p.client?.toLowerCase().includes(search.toLowerCase())
			)
			: projects
	);
</script>

<div class="p-6">
	<div class="flex items-center justify-between mb-6">
		<h1 class="text-2xl font-bold">{$_('project.pageTitle')}</h1>
		<Button data-testid="projects.new" onclick={() => showCreate = true}>
			{$_('project.pageNewProject')}
		</Button>
	</div>

	<div class="mb-4 flex flex-wrap items-center gap-4">
		<input
			bind:value={search}
			placeholder={$_('project.pageSearch')}
			class="w-full max-w-sm border rounded px-3 py-2 text-sm"
		/>
		<label class="flex items-center gap-2 text-sm cursor-pointer">
			<input
				type="checkbox"
				class="accent-brand-green"
				data-testid="projects.only-mine"
				checked={onlyMine}
				onchange={(e) => setOnlyMine(e.currentTarget.checked)}
			/>
			<span>{$_('project.listOnlyMine')}</span>
		</label>
	</div>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	<ProjectList
		projects={filteredProjects}
		{loading}
		error=""
		{onlyMine}
		onshowall={() => setOnlyMine(false)}
	/>
	<CreateProjectDialog bind:open={showCreate} oncreated={loadProjects} />
</div>
