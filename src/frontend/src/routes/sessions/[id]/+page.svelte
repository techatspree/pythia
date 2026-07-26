<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import { _ } from 'svelte-i18n';
	import { getSession } from '$lib/session/api';
	import { connectSessionSocket, type SessionSocketHandle } from '$lib/session/socket';
	import { SessionStore } from '$lib/session/store.svelte';
	import { getAuthProvider } from '$lib/auth';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { log } from '$lib/log';

	// The session room SHELL (task-066): connect the live socket and render the
	// title/status/participants/current-item header + role banner. The phase-1
	// blind form and phase-2 reveal/finalize controls are task-067 — mounted
	// where the placeholder is.
	const sessionId = page.params.id!;
	const subjectId = getAuthProvider().getAccount()?.subjectId ?? null;
	const store = new SessionStore(subjectId);

	let bannerMessage = $state<string | null>(null);
	let handle: SessionSocketHandle | null = null;

	onMount(async () => {
		try {
			store.apply(await getSession(sessionId));
		} catch (e: unknown) {
			log.error('session room: initial load failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
		// Live updates (and the authoritative snapshot on connect) arrive here.
		handle = connectSessionSocket(
			sessionId,
			(s) => store.apply(s),
			(msg) => {
				bannerMessage = msg;
			}
		);
	});

	onDestroy(() => handle?.close());
</script>

<div class="p-6 max-w-4xl mx-auto">
	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	{#if store.session}
		{@const s = store.session}
		<div class="flex items-center gap-3 mb-4">
			<h1 class="text-2xl font-bold">{s.title}</h1>
			<span class="px-2 py-0.5 text-xs rounded-full bg-brand-green/20 text-brand-green"
				>{$_(`session.status.${s.status}`)}</span
			>
			<span
				class="ml-auto flex items-center gap-1.5 text-xs {store.connected
					? 'text-green-600'
					: 'text-gray-400'}"
			>
				<span class="w-2 h-2 rounded-full {store.connected ? 'bg-green-500' : 'bg-gray-300'}"></span>
				{store.connected ? $_('session.layout.connected') : $_('session.layout.connecting')}
			</span>
		</div>

		<p class="text-sm text-gray-600 mb-4">
			{store.isModerator ? $_('session.room.roleModerator') : $_('session.room.roleEstimator')}
		</p>

		<div class="mb-6 border rounded-lg p-4 bg-white">
			<h2 class="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">
				{$_('session.room.participants')}
			</h2>
			<ul class="flex flex-wrap gap-2">
				{#each s.participants as p (p.subjectId)}
					<li class="flex items-center gap-1.5 border rounded px-2 py-1 text-sm bg-gray-50">
						<span>{p.displayName ?? p.subjectId}</span>
						<span class="text-xs text-gray-400 uppercase tracking-wide">{$_(`session.role.${p.role}`)}</span>
						{#if p.agreed}<span class="text-xs text-green-600">✓ {$_('session.room.agreed')}</span>{/if}
					</li>
				{/each}
			</ul>
		</div>

		<div class="border rounded-lg p-4 bg-white">
			<h2 class="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">
				{$_('session.room.currentItem')}
			</h2>
			{#if store.currentItem}
				{@const item = store.currentItem}
				<p class="font-medium">{item.description ?? item.nodeLogicalId}</p>
				<p class="text-sm text-gray-500">{$_(`session.phase.${s.currentPhase}`)}</p>
				<p class="mt-4 text-sm text-gray-400 italic">{$_('session.room.phasePlaceholder')}</p>
			{:else}
				<p class="text-gray-500">{$_('session.room.noCurrentItem')}</p>
			{/if}
		</div>
	{:else}
		<p class="text-gray-500">{$_('session.room.loading')}</p>
	{/if}
</div>
