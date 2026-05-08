<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import ProjectDetail from '$lib/components/ProjectDetail.svelte';

	let project = $state<any | null>(null);
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		const id = page.params.id;
		try {
			const res = await fetch(`/api/projects/${id}`);
			if (!res.ok) throw new Error('Project not found');
			project = await res.json();
		} catch (e: any) {
			error = e.message;
		} finally {
			loading = false;
		}
	});
</script>

<div class="max-w-5xl mx-auto p-6">
	<a href="/projects" class="text-sm text-brand-green hover:underline mb-4 inline-block">&larr; Back to projects</a>
	<ProjectDetail {project} {loading} {error} />
</div>
