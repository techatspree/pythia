<script lang="ts">
	import '../app.css';
	import { onMount } from 'svelte';
	import logo from '$lib/assets/logo.svg';
	import RequireAuth from '$lib/auth/RequireAuth.svelte';
	import AppHeader from '$lib/components/AppHeader.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import ConnectionLostDialog from '$lib/components/ConnectionLostDialog.svelte';
	import { connection } from '$lib/stores/connection.svelte';
	import { getAuthProvider } from '$lib/auth';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { setUserLanguage } from '$lib/i18n';
	import { SupportedLanguage } from '$lib/domain/domain.mjs';
	import { log } from '$lib/log';

	let { children } = $props();

	const provider = getAuthProvider();
	let account = $state<AuthAccount | null>(null);
	let ready = $state(false);
	let initError = $state<string | null>(null);

	// Initialise the active locale from the user's persisted preference once the
	// account resolves (task-125). Maps the ISO code to the domain enum; falls
	// back to DE (dev accounts carry no backend language).
	function applyAccountLanguage(acc: AuthAccount | null) {
		const code = acc?.language ?? 'de';
		const lang = SupportedLanguage.values().find((v) => v.code === code) ?? SupportedLanguage.DE;
		setUserLanguage(lang);
	}

	async function refresh() {
		account = await provider.loadAccount();
		applyAccountLanguage(account);
	}

	onMount(async () => {
		try {
			// Entra/MSAL must be initialized before reading the account (init()
			// also processes the post-login redirect); the dev module's init() is
			// a no-op. loadAccount() then resolves the authoritative account —
			// for Entra from GET /api/auth/me (roles from the backend), for dev
			// client-side. Runs client-side only (ssr is disabled for this layout).
			await provider.init();
			account = await provider.loadAccount();
			applyAccountLanguage(account);
		} catch (e) {
			log.error('loadAccount failed:', e);
			initError = e instanceof Error ? e.message : String(e);
		} finally {
			ready = true;
		}
	});
</script>

<svelte:head>
	<link rel="icon" type="image/svg+xml" href={logo} />
	<link rel="alternate icon" href="/favicon-32.png" />
	<link rel="shortcut icon" href="/favicon.ico" />
	<link rel="apple-touch-icon" href="/apple-touch-icon.png" />
	<link rel="manifest" href="/site.webmanifest" />
</svelte:head>

<AppHeader {account} onlogout={refresh} />

<!-- `main` is deliberately a plain block: making it a flex container turns every
     page's root element into a flex item, whose automatic minimum size stops the
     TreeTable's horizontal scroll wrapper from overflowing (caught by
     `e2e/treetable-responsive.test.ts`). A route that paints its own background
     therefore ends it with its content rather than at the fold. -->
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

<!-- Global connection watchdog: overlays every route (session chrome included). -->
{#if connection.blocked}
	<ConnectionLostDialog />
{/if}
