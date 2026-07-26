<script lang="ts">
	import { onMount } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { _ } from 'svelte-i18n';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { createSession, listSessions, type SessionDto } from '$lib/session/api';

	// Moderator setup (task-066): pick a project → offer, flatten its draft's
	// leaf items into a checkbox picker, name the session, create → navigate to
	// the room. Also lists running sessions of the chosen offer with a join link.
	type Leaf = { logicalId: string; description: string };

	let projects = $state<any[]>([]);
	let projectId = $state('');
	let estimations = $state<any[]>([]);
	let estimationId = $state('');
	let leaves = $state<Leaf[]>([]);
	const selected = new SvelteSet<string>();
	let title = $state('');
	let existing = $state<SessionDto[]>([]);
	let noDraft = $state(false);
	let creating = $state(false);
	let bannerMessage = $state<string | null>(null);

	const canStart = $derived(!!estimationId && title.trim().length > 0 && selected.size > 0 && !creating);

	onMount(async () => {
		try {
			const res = await apiFetch('/api/projects');
			await assertOk(res, $_('session.setup.loadFailed'));
			projects = await res.json();
		} catch (e: unknown) {
			log.error('session setup: load projects failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	});

	async function onProjectChange() {
		estimations = [];
		estimationId = '';
		resetEstimation();
		if (!projectId) return;
		try {
			const res = await apiFetch(`/api/projects/${projectId}`);
			await assertOk(res, $_('session.setup.loadFailed'));
			estimations = (await res.json()).estimations ?? [];
		} catch (e: unknown) {
			log.error('session setup: load project failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	function resetEstimation() {
		leaves = [];
		selected.clear();
		existing = [];
		noDraft = false;
	}

	function collectLeaves(nodes: any[], out: Leaf[]) {
		for (const n of nodes) {
			if (n.type === 'GROUP') collectLeaves(n.children ?? [], out);
			else out.push({ logicalId: n.logicalId, description: n.description || n.logicalId });
		}
	}

	async function onEstimationChange() {
		resetEstimation();
		if (!estimationId) return;
		try {
			const res = await apiFetch(`/api/estimations/${estimationId}/versions/draft`);
			if (res.status === 404) {
				noDraft = true;
				return;
			}
			await assertOk(res, $_('session.setup.loadFailed'));
			const out: Leaf[] = [];
			collectLeaves((await res.json()).roots ?? [], out);
			leaves = out;
			out.forEach((l) => selected.add(l.logicalId));
			existing = await listSessions(estimationId);
		} catch (e: unknown) {
			log.error('session setup: load draft failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	function toggle(id: string) {
		if (selected.has(id)) selected.delete(id);
		else selected.add(id);
	}

	async function start() {
		if (!canStart) return;
		creating = true;
		try {
			const session = await createSession({
				estimationId,
				title: title.trim(),
				itemLogicalIds: [...selected]
			});
			await goto(resolve('/sessions/[id]', { id: session.id }));
		} catch (e: unknown) {
			log.error('session setup: create failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		} finally {
			creating = false;
		}
	}
</script>

<div class="p-6 max-w-2xl mx-auto">
	<h1 class="text-2xl font-bold mb-4">{$_('session.setup.title')}</h1>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	<div class="mb-4">
		<label class="block text-sm font-medium mb-1" for="project">{$_('session.setup.project')}</label>
		<select
			id="project"
			bind:value={projectId}
			onchange={onProjectChange}
			class="w-full border rounded px-3 py-2 text-sm"
		>
			<option value="">{$_('session.setup.selectProject')}</option>
			{#each projects as p (p.id)}
				<option value={p.id}>{p.name}</option>
			{/each}
		</select>
	</div>

	{#if estimations.length > 0}
		<div class="mb-4">
			<label class="block text-sm font-medium mb-1" for="estimation">{$_('session.setup.estimation')}</label>
			<select
				id="estimation"
				bind:value={estimationId}
				onchange={onEstimationChange}
				class="w-full border rounded px-3 py-2 text-sm"
			>
				<option value="">{$_('session.setup.selectEstimation')}</option>
				{#each estimations as e (e.id)}
					<option value={e.id}>{e.offer}</option>
				{/each}
			</select>
		</div>
	{/if}

	{#if noDraft}
		<p class="text-sm text-gray-500 mb-4">{$_('session.setup.noDraft')}</p>
	{/if}

	{#if leaves.length > 0}
		<div class="mb-4">
			<span class="block text-sm font-medium mb-1">{$_('session.setup.items')}</span>
			<div class="border rounded divide-y">
				{#each leaves as leaf (leaf.logicalId)}
					<label class="flex items-center gap-2 px-3 py-2 text-sm cursor-pointer hover:bg-gray-50">
						<input
							type="checkbox"
							class="accent-brand-green"
							checked={selected.has(leaf.logicalId)}
							onchange={() => toggle(leaf.logicalId)}
						/>
						<span>{leaf.description}</span>
					</label>
				{/each}
			</div>
		</div>

		<div class="mb-4">
			<label class="block text-sm font-medium mb-1" for="title">{$_('session.setup.sessionTitle')}</label>
			<input id="title" bind:value={title} class="w-full border rounded px-3 py-2 text-sm" />
		</div>

		<button
			type="button"
			onclick={start}
			disabled={!canStart}
			class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
		>
			{$_('session.setup.start')}
		</button>
	{:else if estimationId && !noDraft}
		<p class="text-sm text-gray-500 mb-4">{$_('session.setup.noItems')}</p>
	{/if}

	{#if existing.length > 0}
		<div class="mt-8">
			<h2 class="text-lg font-semibold mb-2">{$_('session.setup.existing')}</h2>
			<ul class="space-y-2">
				{#each existing as sess (sess.id)}
					<li class="flex items-center gap-3 border rounded px-3 py-2 text-sm">
						<span class="font-medium">{sess.title}</span>
						<span class="text-xs text-gray-400">{$_(`session.status.${sess.status}`)}</span>
						<a
							href={resolve('/sessions/[id]', { id: sess.id })}
							class="ml-auto text-brand-green hover:underline">{$_('session.setup.join')}</a
						>
					</li>
				{/each}
			</ul>
		</div>
	{/if}
</div>
