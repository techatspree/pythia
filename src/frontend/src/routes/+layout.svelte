<script lang="ts">
	import '../app.css';
	import { onMount } from 'svelte';
	import favicon from '$lib/assets/favicon.svg';
	import RequireAuth from '$lib/auth/RequireAuth.svelte';
	import UserMenu from '$lib/components/UserMenu.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { getAuthProvider } from '$lib/auth';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { resolve } from '$app/paths';

	let { children } = $props();

	const provider = getAuthProvider();
	let account = $state<AuthAccount | null>(null);
	let ready = $state(false);
	let initError = $state<string | null>(null);

	function refresh() {
		account = provider.getAccount();
	}

	onMount(async () => {
		try {
			// Entra/MSAL must be initialized before reading the account (init()
			// also processes the post-login redirect); the dev module's init() is
			// a no-op. Runs client-side only (ssr is disabled for this layout).
			await provider.init();
			account = provider.getAccount();
		} catch (e) {
			initError = e instanceof Error ? e.message : String(e);
		} finally {
			ready = true;
		}
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<header class="bg-white">
	<div class="px-6 py-3 flex items-center gap-3">
		<a href={resolve('/projects')}>
			<picture>
				<source srcset="/estimator_logo.webp" type="image/webp" />
				<img
					src="/estimator_logo_w120.png"
					alt="The Estimator"
					width="120"
					height="88"
					loading="eager"
					class="w-30 h-auto"
				/>
			</picture>
		</a>
		<span class="text-brand-green text-xs font-semibold tracking-widest uppercase">Estimator</span>
		{#if account}
			<div class="ml-auto flex items-center gap-3">
				<UserMenu {account} onlogout={refresh} />
			</div>
		{/if}
	</div>
	<div class="h-0.75" style="background: var(--gradient-brand)"></div>
</header>

<main>
	{#if initError}
		<div class="p-6">
			<ErrorBanner message={initError} ondismiss={() => (initError = null)} />
		</div>
	{:else if ready}
		<RequireAuth {account} {refresh} {children} />
	{:else}
		<p class="p-6 text-sm text-gray-600">Loading…</p>
	{/if}
</main>
