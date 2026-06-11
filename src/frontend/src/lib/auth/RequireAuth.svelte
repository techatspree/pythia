<script lang="ts">
	import type { Snippet } from 'svelte';
	import { getAuthProvider } from '$lib/auth';
	import DevLoginDialog from '$lib/components/DevLoginDialog.svelte';

	let { children }: { children: Snippet } = $props();

	let account = $state(getAuthProvider().getAccount());

	function refresh() {
		account = getAuthProvider().getAccount();
	}
</script>

{#if account == null}
	<DevLoginDialog onlogin={refresh} />
{:else}
	{@render children()}
{/if}
