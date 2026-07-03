<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { getAuthProvider } from '$lib/auth';
	import DevLoginDialog from '$lib/components/DevLoginDialog.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';

	let {
		account,
		refresh,
		children
	}: {
		account: AuthAccount | null;
		refresh: () => void;
		children: Snippet;
	} = $props();

	const provider = getAuthProvider();
	let loginError = $state<string | null>(null);
	let loginStarted = $state(false);

	// The dev module uses an in-app user picker (DevLoginDialog). Every other
	// provider (Entra) signs in via its OWN flow — an MSAL redirect to the
	// identity provider — so when unauthenticated we trigger provider.login()
	// instead of showing the dev picker. Failures surface via ErrorBanner.
	$effect(() => {
		if (account == null && provider.name !== 'dev' && !loginStarted) {
			loginStarted = true;
			provider.login().catch((e) => {
				loginError = e instanceof Error ? e.message : String(e);
			});
		}
	});
</script>

{#if account != null}
	{@render children()}
{:else if provider.name === 'dev'}
	<DevLoginDialog onlogin={refresh} />
{:else}
	<div class="p-6">
		<ErrorBanner message={loginError} ondismiss={() => (loginError = null)} />
		{#if loginError == null}
			<p class="text-sm text-gray-600">Redirecting to sign in…</p>
		{/if}
	</div>
{/if}
