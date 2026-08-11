<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import logo from '$lib/assets/logo.svg';
	import UserMenu from '$lib/components/UserMenu.svelte';
	import { system } from '$lib/stores/system.svelte';
	import type { AuthAccount } from '$lib/auth/AuthProvider';

	// The ONE app header (task-141). Every route renders it through the root
	// layout — including the session room, which used to substitute its own
	// chrome and left the user without recognisable navigation. Do not duplicate
	// this markup into a route layout; add a destination to `items` instead.
	let {
		account,
		onlogout
	}: {
		account: AuthAccount | null;
		onlogout: () => void;
	} = $props();

	// `$derived` because the System destination depends on the account's roles:
	// it is ADMIN-only, and `account` arrives asynchronously.
	const items = $derived([
		{ key: 'nav.projects', href: resolve('/projects'), testid: 'nav-projects' },
		{ key: 'nav.sessions', href: resolve('/sessions'), testid: 'nav-sessions' },
		...(account?.roles.includes('ADMIN')
			? [{ key: 'nav.system', href: resolve('/admin/system'), testid: 'nav-system' }]
			: [])
	]);

	// A destination stays marked while the user is anywhere below it, so
	// `/projects/{id}` keeps "Projects" active and a room keeps "Sessions".
	// `/estimations/**` sits under neither item and deliberately matches none.
	function isActive(href: string): boolean {
		return page.url.pathname === href || page.url.pathname.startsWith(href + '/');
	}
</script>

<header class="bg-white">
	<div class="px-6 py-3 flex items-center gap-6">
		<a href={resolve('/projects')} class="flex items-center gap-2 shrink-0">
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
			<!-- The installation's configured name (task-146), falling back to the
			     built-in brand.name when none is set. -->
			<span class="font-heading text-lg tracking-tight text-gray-900" data-testid="brand-name"
				>{system.displayName ?? $_('brand.name')}</span
			>
		</a>
		{#if account}
			<!-- A flat row of links, deliberately not a <ul>/<li> list: the nav is
			     two items, and a page-global `ul li` count is asserted elsewhere. -->
			<nav
				aria-label={$_('nav.mainLabel')}
				data-testid="main-nav"
				class="min-w-0 flex flex-wrap items-center gap-1"
			>
				{#each items as item (item.href)}
					<!-- Every item.href comes from resolve() in the `items` array above;
					     the rule only models a resolve() call in the attribute itself. -->
					<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
					<a href={item.href}
						data-testid={item.testid}
						aria-current={isActive(item.href) ? 'page' : undefined}
						class="inline-block text-sm px-2 py-1 border-b-2 {isActive(item.href)
							? 'text-brand-green border-brand-green'
							: 'text-gray-600 border-transparent hover:text-gray-900'}"
					>
						{$_(item.key)}
					</a>
				{/each}
			</nav>
			<div class="ml-auto flex items-center gap-3">
				<UserMenu {account} {onlogout} />
			</div>
		{/if}
	</div>
	<div class="h-0.75" style="background: var(--gradient-brand)"></div>
</header>
