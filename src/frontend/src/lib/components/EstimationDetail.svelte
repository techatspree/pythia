<script lang="ts">
	import type { ApiEstimationDetail } from '$lib/api/types.js';
	import { formatMethodLabel } from '$lib/methods/labels';
	let { estimation }: { estimation: ApiEstimationDetail } = $props();
</script>

<div class="mb-6">
	<div class="flex items-center gap-3 mb-2">
		<h1 class="text-2xl font-bold">{estimation.offer}</h1>
		{#if estimation.latestVersionNumber}
			<span class="px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green">
				Latest: v{estimation.latestVersionNumber}
			</span>
		{/if}
		<span
			data-testid="estimation-detail.method"
			class="px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green"
		>{formatMethodLabel(estimation.method)}</span>
	</div>
	{#if estimation.description}
		<p class="text-gray-600">{estimation.description}</p>
	{/if}
</div>
