<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { connection } from '$lib/stores/connection.svelte';

	// Blocking modal shown while the backend is unreachable (task-136). It is NOT
	// dismissible by ESC or backdrop click on purpose: the app must stay paused so
	// the user cannot keep editing into a void. The acknowledge button is disabled
	// until the heartbeat confirms the backend is reachable again.
</script>

<div
	class="fixed inset-0 bg-black/60 flex items-center justify-center z-50"
	role="alertdialog"
	aria-modal="true"
	aria-label={$_('connection.title')}
>
	<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
		<h2 class="text-lg font-semibold text-red-700 mb-2">{$_('connection.title')}</h2>
		<p class="text-sm text-gray-700 mb-2">{$_('connection.body')}</p>
		{#if connection.message}
			<p class="text-xs text-gray-400 mb-4">{connection.message}</p>
		{/if}
		{#if !connection.backendAlive}
			<p class="text-sm text-gray-500 mb-4 flex items-center gap-2">
				<span class="inline-block w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
				{$_('connection.reconnecting')}
			</p>
		{/if}
		<button
			type="button"
			onclick={() => connection.acknowledge()}
			disabled={!connection.backendAlive}
			class="w-full px-4 py-2 text-sm bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
		>
			{$_('connection.acknowledge')}
		</button>
	</div>
</div>
