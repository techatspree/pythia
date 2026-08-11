<script lang="ts">
	import Select from '$lib/ui/Select.svelte';
	import Card from '$lib/ui/Card.svelte';
	import Button from '$lib/ui/Button.svelte';
	import { onMount } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { _ } from 'svelte-i18n';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { createSession, listSessions, listJoinableSessions, type SessionDto } from '$lib/session/api';

	// Moderator setup (task-066): pick a project → offer, flatten its draft's
	// leaf items into a checkbox picker, name the session, create → navigate to
	// the room. Also lists running sessions of the chosen offer with a join link.
	// task-128: launchable directly from an offer via ?estimationId=&projectId=,
	// preselects the not-yet-estimated leaves, offers select/deselect-all, and
	// lists all joinable sessions up top for discovery/join.
	type Leaf = {
		logicalId: string;
		description: string;
		/** Ancestor group titles joined with ' / '; empty for a root-level leaf. */
		path: string;
		minEffort: number | null;
		expectedEffort: number | null;
		maxEffort: number | null;
	};

	let projects = $state<any[]>([]);
	let projectId = $state('');
	let estimations = $state<any[]>([]);
	let estimationId = $state('');
	let leaves = $state<Leaf[]>([]);
	const selected = new SvelteSet<string>();
	let title = $state('');
	let moderatorEstimates = $state(true);
	let existing = $state<SessionDto[]>([]);
	let openSessions = $state<SessionDto[]>([]);
	let noDraft = $state(false);
	let creating = $state(false);
	let bannerMessage = $state<string | null>(null);

	const canStart = $derived(!!estimationId && title.trim().length > 0 && selected.size > 0 && !creating);

	// A leaf is "not yet estimated" when its PERT triple is empty (all null-or-0).
	function isUnestimated(leaf: Leaf): boolean {
		return (leaf.minEffort ?? 0) === 0 && (leaf.expectedEffort ?? 0) === 0 && (leaf.maxEffort ?? 0) === 0;
	}

	onMount(async () => {
		try {
			const res = await apiFetch('/api/projects');
			await assertOk(res, $_('session.setup.loadFailed'));
			projects = await res.json();
		} catch (e: unknown) {
			log.error('session setup: load projects failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
		await loadOpenSessions();
		await applyQueryScope();
	});

	async function loadOpenSessions() {
		try {
			openSessions = await listJoinableSessions();
		} catch (e: unknown) {
			log.error('session setup: load open sessions failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	// Pre-scope from the offer's "Start session" link (?projectId=&estimationId=).
	async function applyQueryScope() {
		const qpProject = page.url.searchParams.get('projectId');
		const qpEstimation = page.url.searchParams.get('estimationId');
		if (qpProject) {
			projectId = qpProject;
			await onProjectChange();
		}
		if (qpEstimation) {
			estimationId = qpEstimation;
			await onEstimationChange();
		}
	}

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

	// Carry the ancestor titles down so the picker can show WHERE a leaf sits.
	// Flattening the tree away made two leaves called "Backend" under different
	// groups indistinguishable; the structure is real information, not decoration.
	// `' / '` is the separator the Merlin WBS path already uses.
	function collectLeaves(nodes: any[], out: Leaf[], ancestors: string[] = []) {
		for (const n of nodes) {
			if (n.type === 'GROUP')
				collectLeaves(n.children ?? [], out, [...ancestors, n.title || '']);
			else
				out.push({
					logicalId: n.logicalId,
					description: n.description || n.logicalId,
					path: ancestors.filter(Boolean).join(' / '),
					minEffort: n.minEffort ?? null,
					expectedEffort: n.expectedEffort ?? null,
					maxEffort: n.maxEffort ?? null
				});
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
			// Default-select only the not-yet-estimated leaves (the ones a session
			// is convened to estimate); if all are estimated, select none.
			out.filter(isUnestimated).forEach((l) => selected.add(l.logicalId));
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

	function selectAll() {
		leaves.forEach((l) => selected.add(l.logicalId));
	}

	function deselectAll() {
		selected.clear();
	}

	async function start() {
		if (!canStart) return;
		creating = true;
		try {
			const session = await createSession({
				estimationId,
				title: title.trim(),
				itemLogicalIds: [...selected],
				moderatorEstimates
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

<div class="p-6">
	<h1 class="text-2xl font-bold mb-4">{$_('session.setup.title')}</h1>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	<Card title={$_('session.setup.openSessions')} class="mb-4 max-w-3xl">
			{#if openSessions.length > 0}
				<ul class="space-y-2">
					{#each openSessions as sess (sess.id)}
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
			{:else}
				<p class="text-sm text-gray-500">{$_('session.setup.noOpenSessions')}</p>
			{/if}
	</Card>

	<!-- The setup form is a single narrow column by nature: the page stays
	     full width like every other route, the form constrains itself. -->
	<div class="max-w-2xl">
		<div class="mb-4">
		<label class="block text-sm font-medium mb-1" for="project">{$_('session.setup.project')}</label>
		<Select id="project" bind:value={projectId} onchange={onProjectChange}>
			<option value="">{$_('session.setup.selectProject')}</option>
			{#each projects as p (p.id)}
				<option value={p.id}>{p.name}</option>
			{/each}
		</Select>
	</div>

	{#if estimations.length > 0}
		<div class="mb-4">
			<label class="block text-sm font-medium mb-1" for="estimation">{$_('session.setup.estimation')}</label>
			<Select id="estimation" bind:value={estimationId} onchange={onEstimationChange}>
				<option value="">{$_('session.setup.selectEstimation')}</option>
				{#each estimations as e (e.id)}
					<option value={e.id}>{e.offer}</option>
				{/each}
			</Select>
		</div>
	{/if}

	{#if noDraft}
		<p class="text-sm text-gray-500 mb-4">{$_('session.setup.noDraft')}</p>
	{/if}

	{#if leaves.length > 0}
		<div class="mb-4">
			<span class="block text-sm font-medium mb-1">{$_('session.setup.items')}</span>
			<div class="border rounded overflow-hidden">
				<!-- Header strip: the app's sub-section idiom, carrying the live count
				     and the bulk controls. -->
				<div class="flex items-center gap-3 px-3 py-2 bg-gray-50/40 border-b">
					<span class="text-xs text-gray-500">
						{$_('session.setup.selectedCount', {
							values: { selected: selected.size, total: leaves.length }
						})}
					</span>
					<div class="ml-auto flex items-center gap-3 text-xs">
						<button type="button" onclick={selectAll} class="text-brand-green hover:underline"
							>{$_('session.setup.selectAll')}</button
						>
						<button type="button" onclick={deselectAll} class="text-brand-green hover:underline"
							>{$_('session.setup.deselectAll')}</button
						>
					</div>
				</div>
				<!-- Capped and scrollable: a real draft has dozens of leaves, and an
				     uncapped list pushed the title field, the moderator checkbox and
				     the start button below the fold — the form looked submit-less. -->
				<div class="max-h-72 overflow-y-auto divide-y">
					{#each leaves as leaf (leaf.logicalId)}
						{@const isSelected = selected.has(leaf.logicalId)}
						<label
							class="flex items-start gap-2 px-3 py-2 text-sm cursor-pointer hover:bg-gray-50 {isSelected
								? 'bg-brand-green/5'
								: ''}"
						>
							<input
								type="checkbox"
								class="accent-brand-green mt-0.5"
								checked={isSelected}
								onchange={() => toggle(leaf.logicalId)}
							/>
							<span class="min-w-0">
								<span class="block">{leaf.description}</span>
								{#if leaf.path}
									<span class="block text-xs text-gray-400">{leaf.path}</span>
								{/if}
							</span>
							{#if !isUnestimated(leaf)}
								<!-- Neutral grey, NOT brand-green: every brand-green chip in this
								     app means "current/active"; this means "already done". -->
								<span
									class="ml-auto shrink-0 px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-500"
									>{$_('session.setup.estimated')}</span
								>
							{/if}
						</label>
					{/each}
				</div>
			</div>
		</div>

		<div class="mb-4">
			<label class="block text-sm font-medium mb-1" for="title">{$_('session.setup.sessionTitle')}</label>
			<input id="title" bind:value={title} class="w-full border rounded px-3 py-2 text-sm" />
		</div>

		<label class="flex items-center gap-2 mb-4 text-sm cursor-pointer">
			<input type="checkbox" class="accent-brand-green" bind:checked={moderatorEstimates} />
			<span>{$_('session.setup.moderatorEstimates')}</span>
		</label>

		<div class="flex items-center gap-3">
			<Button onclick={start} disabled={!canStart}>
				{$_('session.setup.start')}
			</Button>
			<!-- Say WHY the button is dead rather than leaving the user to guess. -->
			{#if selected.size === 0}
				<span class="text-xs text-gray-500">{$_('session.setup.noneSelected')}</span>
			{/if}
		</div>
	{:else if estimationId && !noDraft}
		<p class="text-sm text-gray-500 mb-4">{$_('session.setup.noItems')}</p>
	{/if}
	</div>

	{#if existing.length > 0}
		<Card title={$_('session.setup.existing')} class="mt-8 max-w-3xl">
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
		</Card>
	{/if}
</div>
