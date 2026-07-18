<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import RequiredRole from '$lib/auth/RequiredRole.svelte';
	import CreateEstimationDialog from '$lib/components/CreateEstimationDialog.svelte';
	import type { components } from '$lib/api/schema';

	type EstimationSummaryDto = components['schemas']['EstimationSummaryDto'];

	let { project, loading, error }: { project: any | null; loading: boolean; error: string } = $props();

	let dialogOpen = $state(false);

	function handleCreated(created: EstimationSummaryDto) {
		if (project && Array.isArray(project.estimations)) {
			project.estimations = [...project.estimations, created];
		}
		if (created.id) {
			goto(resolve('/estimations/[id]', { id: created.id }));
		}
	}
</script>

{#if loading}
	<p class="text-gray-500">Loading project...</p>
{:else if error}
	<p class="text-red-600">{error}</p>
{:else if project}
	<div class="mb-6">
		<div class="flex items-center gap-3 mb-2">
			<h1 class="text-2xl font-bold">{project.name}</h1>
			<span class="px-2 py-0.5 text-xs rounded-full {project.status === 'ACTIVE' ? 'bg-brand-green/20 text-brand-green' : 'bg-gray-100 text-gray-600'}">
				{project.status}
			</span>
		</div>
		{#if project.description}
			<p class="text-gray-600 mb-1">{project.description}</p>
		{/if}
		{#if project.client}
			<p class="text-sm text-gray-500">Client: {project.client}</p>
		{/if}
	</div>

	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">Estimations</h2>
		<RequiredRole role="ESTIMATOR">
			<button
				type="button"
				onclick={() => (dialogOpen = true)}
				class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
			>
				Neue Kalkulation
			</button>
		</RequiredRole>
	</div>

	{#if project.estimations.length === 0}
		<p class="text-gray-500">No estimations yet.</p>
	{:else}
		<div class="overflow-x-auto">
			<table class="w-full text-sm text-left">
				<thead class="text-xs uppercase bg-brand-green/10 border-b text-brand-green">
					<tr>
						<th class="px-4 py-3">Offer</th>
						<th class="px-4 py-3">Description</th>
						<th class="px-4 py-3">Versions</th>
						<th class="px-4 py-3">Current Version</th>
						<th class="px-4 py-3">Created</th>
					</tr>
				</thead>
				<tbody>
					{#each project.estimations as estimation (estimation.id)}
						<tr class="border-b hover:bg-gray-50">
							<td class="px-4 py-3 font-medium">
								<a href={resolve('/estimations/[id]', { id: estimation.id })} class="text-brand-green hover:underline">{estimation.offer}</a>
							</td>
							<td class="px-4 py-3 text-gray-600">{estimation.description || '—'}</td>
							<td class="px-4 py-3">{estimation.versionCount}</td>
							<td class="px-4 py-3">
								{#if estimation.latestVersionNumber}
									v{estimation.latestVersionNumber}
								{:else}
									—
								{/if}
							</td>
							<td class="px-4 py-3 text-gray-500">
								{estimation.createdAt ? new Date(estimation.createdAt).toLocaleDateString() : '—'}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}

	<CreateEstimationDialog bind:open={dialogOpen} projectId={project.id} oncreated={handleCreated} />
{/if}
