<script lang="ts">
	let { versions, estimationId, oncreate }: { versions: any[]; estimationId: string; oncreate: () => void } = $props();

	const statusColors: Record<string, string> = {
		DRAFT: 'bg-yellow-100 text-yellow-800',
		SUBMITTED: 'bg-green-100 text-green-800',
		APPROVED: 'bg-blue-100 text-blue-800',
		REJECTED: 'bg-red-100 text-red-800'
	};
</script>

<div class="mt-6">
	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">Versions</h2>
		<button onclick={oncreate} class="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">
			Create new version
		</button>
	</div>

	{#if versions.length === 0}
		<p class="text-gray-500">No versions yet. Create one to get started.</p>
	{:else}
		<div class="space-y-3">
			{#each versions as version}
				<a
					href="/estimations/{estimationId}/versions/{version.versionNumber}"
					class="block border rounded-lg p-4 hover:bg-gray-50 transition-colors"
				>
					<div class="flex items-center justify-between">
						<div class="flex items-center gap-3">
							<span class="text-lg font-medium">v{version.versionNumber}</span>
							<span class="px-2 py-0.5 text-xs rounded-full {statusColors[version.status] || 'bg-gray-100 text-gray-600'}">
								{version.status}
							</span>
						</div>
						<span class="text-sm text-gray-500">
							{version.createdAt ? new Date(version.createdAt).toLocaleDateString() : ''}
						</span>
					</div>
					<div class="mt-1 flex items-center gap-4 text-sm text-gray-600">
						{#if version.totalEffort != null}
							<span>Effort: {version.totalEffort} PT</span>
						{/if}
						{#if version.notes}
							<span class="truncate">{version.notes}</span>
						{/if}
					</div>
				</a>
			{/each}
		</div>
	{/if}
</div>
