<script lang="ts">
	let { estimation }: { estimation: any } = $props();
</script>

<div class="mb-6">
	<div class="flex items-center gap-3 mb-2">
		<h1 class="text-2xl font-bold">{estimation.offer}</h1>
		{#if estimation.latestVersionNumber}
			<span class="px-2 py-0.5 text-xs rounded-full bg-blue-100 text-blue-800">
				Latest: v{estimation.latestVersionNumber}
			</span>
		{/if}
	</div>
	{#if estimation.description}
		<p class="text-gray-600">{estimation.description}</p>
	{/if}
</div>
