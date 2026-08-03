<script lang="ts">
	import '../app.css';
	import { onMount } from 'svelte';
	import logo from '$lib/assets/logo.svg';
	import RequireAuth from '$lib/auth/RequireAuth.svelte';
	import UserMenu from '$lib/components/UserMenu.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { getAuthProvider } from '$lib/auth';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { setUserLanguage } from '$lib/i18n';
	import { SupportedLanguage } from '$lib/domain/domain.mjs';
	import { log } from '$lib/log';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { _ } from 'svelte-i18n';

	let { children } = $props();

	// The collaborative-session route group (task-066) has its own full-screen
	// "room" chrome, so the standard header is hidden there (auth + i18n gate
	// below still apply — the session layout is nested inside this one).
	const isSessionRoute = $derived(page.url.pathname.startsWith('/sessions'));

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

{#if !isSessionRoute}
	<header class="bg-white">
		<div class="px-6 py-3 flex items-center gap-3">
			<a href={resolve('/projects')} class="flex items-center gap-2">
				<!-- Decorative: the wordmark beside it already announces the product
				     name, so a non-empty alt would be read out twice. -->
				<img
					src={logo}
					alt=""
					aria-hidden="true"
					width="36"
					height="36"
					loading="eager"
					class="w-9 h-9"
					data-testid="brand-logo"
				/>
				<span class="font-heading text-lg tracking-tight text-gray-900">{$_('brand.name')}</span>
			</a>
			{#if account}
				<a
					href={resolve('/sessions')}
					class="text-sm text-brand-green hover:text-[#007a45] underline-offset-2 hover:underline"
					>{$_('nav.sessions')}</a
				>
				<div class="ml-auto flex items-center gap-3">
					<UserMenu {account} onlogout={refresh} />
				</div>
			{/if}
		</div>
		<div class="h-0.75" style="background: var(--gradient-brand)"></div>
	</header>
{/if}

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
