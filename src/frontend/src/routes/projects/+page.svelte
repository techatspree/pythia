<script lang="ts">
	import { onMount } from 'svelte';
	import ProjectList from '$lib/components/ProjectList.svelte';
	import CreateProjectDialog from '$lib/components/CreateProjectDialog.svelte';

	let projects = $state<any[]>([]);
	let loading = $state(true);
	let error = $state('');
	let showCreate = $state(false);
	let search = $state('');

	async function loadProjects() {
		loading = true;
		error = '';
		try {
			const res = await fetch('/api/projects');
			if (!res.ok) throw new Error('Failed to load projects');
			projects = await res.json();
		} catch (e: any) {
			error = e.message;
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

<div class="max-w-5xl mx-auto p-6">
	<div class="flex items-center justify-between mb-6">
		<h1 class="text-2xl font-bold">Projects</h1>
		<button onclick={() => showCreate = true} class="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">
			New Project
		</button>
	</div>

	<div class="mb-4">
		<input
			bind:value={search}
			placeholder="Search projects..."
			class="w-full max-w-sm border rounded px-3 py-2 text-sm"
		/>
	</div>

	<ProjectList projects={filteredProjects} {loading} {error} />
	<CreateProjectDialog bind:open={showCreate} oncreated={loadProjects} />
</div>
