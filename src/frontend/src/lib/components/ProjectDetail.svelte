<script lang="ts">
	import Badge from '$lib/ui/Badge.svelte';
	import Button from '$lib/ui/Button.svelte';
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { _, locale } from 'svelte-i18n';
	import { formatDate, DEFAULT_LOCALE } from '$lib/format';
	import RequiredRole from '$lib/auth/RequiredRole.svelte';
	import CreateEstimationDialog from '$lib/components/CreateEstimationDialog.svelte';
	import ChangeOwnerDialog from '$lib/components/ChangeOwnerDialog.svelte';
	import { formatMethodLabel } from '$lib/methods/labels';
	import type { components } from '$lib/api/schema';
	import type { ApiProjectDetail } from '$lib/api/types';

	type EstimationSummaryDto = components['schemas']['EstimationSummaryDto'];

	let { project, loading, error }: { project: any | null; loading: boolean; error: string } = $props();

	let dialogOpen = $state(false);
	let ownerDialogOpen = $state(false);

	// The response carries the backend's fresh `canChangeOwner`: after handing a
	// project away, the caller usually loses the button.
	function handleOwnerChanged(updated: ApiProjectDetail) {
		project.owner = updated.owner;
		project.canChangeOwner = updated.canChangeOwner;
	}

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
	<p class="text-ink-muted">{$_('project.detailLoading')}</p>
{:else if error}
	<p class="text-red-600">{error}</p>
{:else if project}
	<div class="mb-6">
		<div class="flex items-center gap-3 mb-2">
			<h1 class="text-2xl font-bold">{project.name}</h1>
			<Badge variant={project.status === 'ACTIVE' ? 'brand' : 'neutral'}>
				{project.status}
			</Badge>
		</div>
		{#if project.description}
			<p class="text-ink-muted mb-1">{project.description}</p>
		{/if}
		{#if project.client}
			<p class="text-sm text-ink-muted">{$_('project.detailClient', { values: { client: project.client } })}</p>
		{/if}
		<div class="flex items-center gap-3 mt-1">
			<p class="text-sm text-ink-muted" data-testid="project-detail.owner">
				{project.owner?.displayName
					? $_('project.detailOwner', { values: { owner: project.owner.displayName } })
					: $_('project.detailOwnerNone')}
			</p>
			{#if project.canChangeOwner}
				<Button
					variant="secondary"
					size="xs"
					data-testid="project-detail.change-owner"
					onclick={() => (ownerDialogOpen = true)}
				>
					{$_('project.detailChangeOwner')}
				</Button>
			{/if}
		</div>
	</div>

	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">{$_('project.detailEstimations')}</h2>
		<RequiredRole role="ESTIMATOR">
			<Button data-testid="project-detail.new-offer" onclick={() => (dialogOpen = true)}>
				{$_('project.detailNewOffer')}
			</Button>
		</RequiredRole>
	</div>

	{#if project.estimations.length === 0}
		<p class="text-ink-muted">{$_('project.detailEmpty')}</p>
	{:else}
		<div class="overflow-x-auto">
			<table class="w-full text-sm text-left">
				<thead class="text-xs uppercase bg-brand-green/10 border-b text-brand-green">
					<tr>
						<th class="px-4 py-3">{$_('project.detailColOffer')}</th>
						<th class="px-4 py-3">{$_('project.detailColDescription')}</th>
						<th class="px-4 py-3">{$_('project.detailColMethod')}</th>
						<th class="px-4 py-3">{$_('project.detailColVersions')}</th>
						<th class="px-4 py-3">{$_('project.detailColCurrentVersion')}</th>
						<th class="px-4 py-3">{$_('project.detailColCreated')}</th>
					</tr>
				</thead>
				<tbody>
					{#each project.estimations as estimation (estimation.id)}
						<tr class="border-b hover:bg-surface-subtle">
							<td class="px-4 py-3 font-medium">
								<a href={resolve('/estimations/[id]', { id: estimation.id })} class="text-brand-green hover:underline">{estimation.offer}</a>
							</td>
							<td class="px-4 py-3 text-ink-muted">{estimation.description || '—'}</td>
							<td class="px-4 py-3">
								<Badge
									data-testid="project-detail.estimation-method"
									variant="brand"
								>{formatMethodLabel(estimation.method)}</Badge>
							</td>
							<td class="px-4 py-3">{estimation.versionCount}</td>
							<td class="px-4 py-3">
								{#if estimation.latestVersionNumber}
									v{estimation.latestVersionNumber}
								{:else}
									—
								{/if}
							</td>
							<td class="px-4 py-3 text-ink-muted">
								{estimation.createdAt ? formatDate(estimation.createdAt, $locale ?? DEFAULT_LOCALE) : '—'}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}

	<CreateEstimationDialog bind:open={dialogOpen} projectId={project.id} oncreated={handleCreated} />
	<ChangeOwnerDialog
		bind:open={ownerDialogOpen}
		projectId={project.id}
		currentOwnerId={project.owner?.id ?? null}
		onchanged={handleOwnerChanged}
	/>
{/if}
