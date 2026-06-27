<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import ProjectDetail from '$lib/components/ProjectDetail.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';

	let project = $state<any | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	onMount(async () => {
		const id = page.params.id;
		try {
			const res = await fetch(`/api/projects/${id}`);
			if (!res.ok) throw new Error('Project not found');
			project = await res.json();
		} catch (e: any) {
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	});
</script>

<div class="p-6">
	<a href={resolve('/projects')} class="text-sm text-brand-green hover:underline mb-4 inline-block">&larr; Back to projects</a>
	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
	<ProjectDetail {project} {loading} error="" />
</div>
