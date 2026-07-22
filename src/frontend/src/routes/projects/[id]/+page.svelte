<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { _ } from 'svelte-i18n';
	import ProjectDetail from '$lib/components/ProjectDetail.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';

	let project = $state<any | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	onMount(async () => {
		const id = page.params.id;
		try {
			const res = await apiFetch(`/api/projects/${id}`);
			await assertOk(res, $_('project.pageNotFound'));
			project = await res.json();
		} catch (e: any) {
			log.error('load project failed:', e);
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	});
</script>

<div class="p-6">
	<a href={resolve('/projects')} class="text-sm text-brand-green hover:underline mb-4 inline-block">{$_('project.pageBack')}</a>
	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
	<ProjectDetail {project} {loading} error="" />
</div>
