<script lang="ts">
	import { onMount } from 'svelte';
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

	async function loadProjects() {
		loading = true;
		bannerMessage = null;
		try {
			const res = await apiFetch('/api/projects');
			await assertOk(res, $_('project.pageLoadFailed'));
			projects = await res.json();
		} catch (e: any) {
			log.error('loadProjects failed:', e);
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	}

	onMount(loadProjects);

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
		<button onclick={() => showCreate = true} class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
			{$_('project.pageNewProject')}
		</button>
	</div>

	<div class="mb-4">
		<input
			bind:value={search}
			placeholder={$_('project.pageSearch')}
			class="w-full max-w-sm border rounded px-3 py-2 text-sm"
		/>
	</div>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	<ProjectList projects={filteredProjects} {loading} error="" />
	<CreateProjectDialog bind:open={showCreate} oncreated={loadProjects} />
</div>
