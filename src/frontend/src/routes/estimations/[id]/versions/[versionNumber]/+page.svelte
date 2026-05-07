<script lang="ts">
	import { page } from '$app/state';

	const params = page.params as { id: string; versionNumber: string };
</script>

<div class="max-w-5xl mx-auto p-6">
	<a href="/estimations/{params.id}" class="text-sm text-blue-600 hover:underline mb-4 inline-block">&larr; Back to estimation</a>
	<h1 class="text-2xl font-bold mb-4">Version {params.versionNumber}</h1>
	<p class="text-gray-500">Version detail — coming soon</p>
</div>
