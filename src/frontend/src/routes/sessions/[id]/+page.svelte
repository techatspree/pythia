<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { _ } from 'svelte-i18n';
	import { getSession, join, start, cancel } from '$lib/session/api';
	import { connectSessionSocket, type SessionSocketHandle } from '$lib/session/socket';
	import { SessionStore } from '$lib/session/store.svelte';
	import { getAuthProvider } from '$lib/auth';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import PhaseOnePanel from '$lib/session/PhaseOnePanel.svelte';
	import PhaseTwoPanel from '$lib/session/PhaseTwoPanel.svelte';
	import SessionSummary from '$lib/session/SessionSummary.svelte';
	import { log } from '$lib/log';

	// The session room (task-067): connect the live socket, auto-join as an
	// estimator, and render the phase-appropriate panel — PHASE1 blind input,
	// PHASE2 reveal/finalize, or the FINALIZED summary — driven by the pushed
	// SessionDto store.
	const sessionId = page.params.id!;
	const subjectId = getAuthProvider().getAccount()?.subjectId ?? null;
	const store = new SessionStore(subjectId);

	let bannerMessage = $state<string | null>(null);
	let handle: SessionSocketHandle | null = null;

	function onError(msg: string) {
		bannerMessage = msg;
	}

	onMount(async () => {
		try {
			store.apply(await getSession(sessionId));
			// Join as an estimator if not already a participant and the session is
			// still joinable (idempotent server-side; moderator is already in).
			const s = store.session;
			if (s && !store.myParticipant && (s.status === 'CREATED' || s.status === 'RUNNING')) {
				store.apply(await join(sessionId));
			}
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

	async function startSession() {
		try {
			store.apply(await start(sessionId));
		} catch (e: unknown) {
			log.error('session room: start failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	async function cancelSession() {
		try {
			store.apply(await cancel(sessionId));
		} catch (e: unknown) {
			log.error('session room: cancel failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}
</script>

<div class="p-6 max-w-4xl mx-auto">
	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	{#if store.session}
		{@const s = store.session}
		<a
			href={resolve('/estimations/[id]', { id: s.estimationId })}
			class="text-sm text-brand-green hover:underline mb-4 inline-block"
			>{$_('session.room.backToEstimation')}</a
		>
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
					<li
						data-testid="participant"
						class="flex items-center gap-1.5 border rounded px-2 py-1 text-sm bg-gray-50"
					>
						<span>{p.displayName ?? p.subjectId}</span>
						<span class="text-xs text-gray-400 uppercase tracking-wide">{$_(`session.role.${p.role}`)}</span>
						{#if p.agreed}<span class="text-xs text-green-600">✓ {$_('session.room.agreed')}</span>{/if}
					</li>
				{/each}
			</ul>
		</div>

		{#if s.status === 'CANCELLED'}
			<div class="border rounded-lg p-4 bg-white text-gray-600">
				{$_('session.room.cancelled')}
			</div>
		{:else if s.status === 'FINALIZED'}
			<SessionSummary {store} />
		{:else if s.status === 'CREATED'}
			<div class="border rounded-lg p-4 bg-white">
				{#if store.isModerator}
					<p class="text-sm text-gray-600 mb-3">{$_('session.room.startHint')}</p>
					<div class="flex items-center gap-3">
						<button
							type="button"
							onclick={startSession}
							class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
						>
							{$_('session.room.start')}
						</button>
						<button
							type="button"
							onclick={cancelSession}
							class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700"
						>
							{$_('session.room.cancel')}
						</button>
					</div>
				{:else}
					<p class="text-gray-500">{$_('session.room.waitingToStart')}</p>
				{/if}
			</div>
		{:else}
			<!-- RUNNING -->
			<div class="border rounded-lg p-4 bg-white">
				<div class="flex items-center gap-3 mb-1">
					<h2 class="text-xs font-semibold uppercase tracking-wide text-gray-500">
						{$_('session.room.currentItem')}
					</h2>
					<span class="text-xs text-gray-400">
						{$_('session.room.itemPosition', {
							values: { position: s.currentItemIndex + 1, total: s.items.length }
						})}
					</span>
					<span class="ml-auto px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600">
						{$_(`session.phase.${s.currentPhase}`)}
					</span>
				</div>
				{#if store.currentItem}
					{@const item = store.currentItem}
					<p class="font-medium mb-4">{item.description ?? item.nodeLogicalId}</p>
					{#if s.currentPhase === 'PHASE1'}
						<PhaseOnePanel {store} {sessionId} {onError} />
					{:else}
						<PhaseTwoPanel {store} {sessionId} {onError} />
					{/if}
				{:else}
					<p class="text-gray-500">{$_('session.room.noCurrentItem')}</p>
				{/if}
			</div>
		{/if}
	{:else}
		<p class="text-gray-500">{$_('session.room.loading')}</p>
	{/if}
</div>
