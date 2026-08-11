<script lang="ts">
	import Badge from '$lib/ui/Badge.svelte';
	import { resolve } from '$app/paths';
	import { _, locale } from 'svelte-i18n';
	import { formatDate, DEFAULT_LOCALE } from '$lib/format';
	let { projects, loading, error }: { projects: any[]; loading: boolean; error: string } = $props();
</script>

{#if loading}
	<p class="text-gray-500">{$_('project.listLoading')}</p>
{:else if error}
	<p class="text-red-600">{error}</p>
{:else if projects.length === 0}
	<p class="text-gray-500">{$_('project.listEmpty')}</p>
{:else}
	<div class="overflow-x-auto">
		<table class="w-full text-sm text-left">
			<thead class="text-xs uppercase bg-brand-green/10 border-b text-brand-green">
				<tr>
					<th class="px-4 py-3">{$_('project.listColName')}</th>
					<th class="px-4 py-3">{$_('project.listColClient')}</th>
					<th class="px-4 py-3">{$_('project.listColStatus')}</th>
					<th class="px-4 py-3">{$_('project.listColCreated')}</th>
				</tr>
			</thead>
			<tbody>
				{#each projects as project (project.id)}
					<tr class="border-b hover:bg-gray-50">
						<td class="px-4 py-3">
							<a href={resolve('/projects/[id]', { id: project.id })} class="text-brand-green hover:underline font-medium">
								{project.name}
							</a>
							{#if project.description}
								<p class="text-xs text-gray-500 mt-0.5">{project.description}</p>
							{/if}
						</td>
						<td class="px-4 py-3">{project.client || '—'}</td>
						<td class="px-4 py-3">
							<Badge variant={project.status === 'ACTIVE' ? 'brand' : 'neutral'}>
								{project.status}
							</Badge>
						</td>
						<td class="px-4 py-3 text-gray-500">
							{project.createdAt ? formatDate(project.createdAt, $locale ?? DEFAULT_LOCALE) : '—'}
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{/if}
