<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';
	import RequireAuth from '$lib/auth/RequireAuth.svelte';
	import UserMenu from '$lib/components/UserMenu.svelte';
	import { getAuthProvider } from '$lib/auth';
	import { resolve } from '$app/paths';

	let { children } = $props();

	let account = $state(getAuthProvider().getAccount());

	function refresh() {
		account = getAuthProvider().getAccount();
	}
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
					src="/estimator_logo.png"
					alt="The Estimator"
					width="120"
					height="88"
					loading="eager"
					class="w-[120px] h-auto"
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
	<div class="h-[3px]" style="background: var(--gradient-brand)"></div>
</header>

<main>
	<RequireAuth {account} {refresh} {children} />
</main>
