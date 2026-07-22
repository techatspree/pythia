<script lang="ts">
	import { _ } from 'svelte-i18n';

	let { message, ondismiss }: {
		message: string | null;
		ondismiss?: () => void;
	} = $props();
</script>

{#if message}
	<div
		role="alert"
		class="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded text-red-800 text-sm flex items-start gap-3"
	>
		<span class="flex-1">{message}</span>
		{#if ondismiss}
			<button
				type="button"
				aria-label={$_('common.dismiss')}
				class="text-red-800 hover:text-red-900 leading-none text-lg"
				onclick={() => ondismiss?.()}
			>×</button>
		{/if}
	</div>
{/if}
