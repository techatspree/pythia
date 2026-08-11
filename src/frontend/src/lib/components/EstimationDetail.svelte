<script lang="ts">
	import Badge from '$lib/ui/Badge.svelte';
	import { _ } from 'svelte-i18n';
	import type { ApiEstimationDetail } from '$lib/api/types.js';
	import { formatMethodLabel } from '$lib/methods/labels';
	import MethodDetailPopover from '$lib/methods/MethodDetailPopover.svelte';
	let { estimation }: { estimation: ApiEstimationDetail } = $props();

	let popoverOpen = $state(false);
</script>

<div class="mb-6">
	<div class="flex items-center gap-3 mb-2">
		<h1 class="text-2xl font-bold">{estimation.offer}</h1>
		{#if estimation.latestVersionNumber}
			<Badge variant="brand">
				{$_('estimation.detailLatest', { values: { n: estimation.latestVersionNumber } })}
			</Badge>
		{/if}
		<div class="relative">
			<button
				type="button"
				data-testid="estimation-detail.method"
				onclick={() => (popoverOpen = !popoverOpen)}
				class="px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green hover:bg-brand-green/30 focus:outline-none focus:ring-2 focus:ring-brand-green/40"
			>{formatMethodLabel(estimation.method)}</button>
			<MethodDetailPopover
				bind:open={popoverOpen}
				method={estimation.method}
				description={estimation.methodDescription}
			/>
		</div>
	</div>
	{#if estimation.description}
		<p class="text-gray-600">{estimation.description}</p>
	{/if}
</div>
