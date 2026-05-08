<script lang="ts">
	let { projects, loading, error }: { projects: any[]; loading: boolean; error: string } = $props();
</script>

{#if loading}
	<p class="text-gray-500">Loading projects...</p>
{:else if error}
	<p class="text-red-600">{error}</p>
{:else if projects.length === 0}
	<p class="text-gray-500">No projects yet. Create one to get started.</p>
{:else}
	<div class="overflow-x-auto">
		<table class="w-full text-sm text-left">
			<thead class="text-xs uppercase bg-brand-green/10 border-b text-brand-green">
				<tr>
					<th class="px-4 py-3">Name</th>
					<th class="px-4 py-3">Client</th>
					<th class="px-4 py-3">Status</th>
					<th class="px-4 py-3">Created</th>
				</tr>
			</thead>
			<tbody>
				{#each projects as project}
					<tr class="border-b hover:bg-gray-50">
						<td class="px-4 py-3">
							<a href="/projects/{project.id}" class="text-brand-green hover:underline font-medium">
								{project.name}
							</a>
							{#if project.description}
								<p class="text-xs text-gray-500 mt-0.5">{project.description}</p>
							{/if}
						</td>
						<td class="px-4 py-3">{project.client || '—'}</td>
						<td class="px-4 py-3">
							<span class="px-2 py-0.5 text-xs rounded-full {project.status === 'ACTIVE' ? 'bg-brand-green/20 text-brand-green' : 'bg-gray-100 text-gray-600'}">
								{project.status}
							</span>
						</td>
						<td class="px-4 py-3 text-gray-500">
							{project.createdAt ? new Date(project.createdAt).toLocaleDateString() : '—'}
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{/if}
