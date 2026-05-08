<script lang="ts">
	let { versions, estimationId, oncreate, onsubmit }: { versions: any[]; estimationId: string; oncreate: () => void; onsubmit: () => void } = $props();

	function statusLabel(version: any): string {
		return version.isDraft ? 'DRAFT' : 'SUBMITTED';
	}

	function statusColor(version: any): string {
		return version.isDraft ? 'bg-yellow-100 text-yellow-800' : 'bg-brand-green/20 text-brand-green';
	}
</script>

<div class="mt-6">
	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">Versions</h2>
		<button onclick={oncreate} class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
			Create new version
		</button>
	</div>

	{#if versions.length === 0}
		<p class="text-gray-500">No versions yet. Create one to get started.</p>
	{:else}
		<div class="space-y-3">
			{#each versions as version}
				<a
					href="/estimations/{estimationId}/versions/{version.versionNumber}{version.isDraft ? '?draft=true' : ''}"
					class="block border rounded-lg p-4 hover:bg-gray-50 transition-colors"
				>
					<div class="flex items-center justify-between">
						<div class="flex items-center gap-3">
							<span class="text-lg font-medium">v{version.versionNumber}</span>
							<span class="px-2 py-0.5 text-xs rounded-full {statusColor(version)}">
								{statusLabel(version)}
							</span>
						</div>
						<div class="flex items-center gap-3">
							{#if version.isDraft}
								<button
									onclick={(e: MouseEvent) => { e.preventDefault(); onsubmit(); }}
									class="px-3 py-1 text-xs bg-brand-green text-white rounded hover:bg-[#007a45]"
								>
									Submit
								</button>
							{/if}
							<span class="text-sm text-gray-500">
								{version.createdAt ? new Date(version.createdAt).toLocaleDateString() : ''}
							</span>
						</div>
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
