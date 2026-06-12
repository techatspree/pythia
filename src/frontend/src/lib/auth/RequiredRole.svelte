<script lang="ts">
	import type { Snippet } from 'svelte';
	import { getAuthProvider } from '$lib/auth';

	let {
		role,
		children
	}: {
		role: 'VIEWER' | 'ESTIMATOR' | 'ADMIN';
		children: Snippet;
	} = $props();

	let account = $state(getAuthProvider().getAccount());

	export function refresh() {
		account = getAuthProvider().getAccount();
	}
</script>

{#if account && account.roles.includes(role)}
	{@render children()}
{:else}
	<div class="text-sm text-gray-400 italic">Insufficient role ({role})</div>
{/if}
