<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import DevLoginDialog from '$lib/components/DevLoginDialog.svelte';

	let {
		account,
		refresh,
		children
	}: {
		account: AuthAccount | null;
		refresh: () => void;
		children: Snippet;
	} = $props();
</script>

{#if account == null}
	<DevLoginDialog onlogin={refresh} />
{:else}
	{@render children()}
{/if}
