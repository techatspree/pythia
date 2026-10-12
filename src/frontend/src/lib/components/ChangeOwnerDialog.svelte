<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import Field from '$lib/ui/Field.svelte';
	import Select from '$lib/ui/Select.svelte';
	import { tick } from 'svelte';
	import { _ } from 'svelte-i18n';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';
	import type { ApiProjectDetail, ApiUserRef } from '$lib/api/types';

	// Transfers a project to another person (task-200). Whether the caller may do
	// this is the backend's call (`canChangeOwner`); this dialog is only shown
	// when it said yes, and a 403 still lands in the banner.
	let {
		open = $bindable(false),
		projectId,
		currentOwnerId,
		onchanged
	}: {
		open: boolean;
		projectId: string;
		currentOwnerId: string | null;
		onchanged: (project: ApiProjectDetail) => void;
	} = $props();

	const SELECT_ID = 'change-owner-select';

	let users = $state<ApiUserRef[]>([]);
	let selected = $state('');
	let saving = $state(false);
	let bannerMessage = $state<string | null>(null);

	// Only a DIFFERENT person is a change.
	let canSave = $derived(selected !== '' && selected !== (currentOwnerId ?? '') && !saving);

	$effect(() => {
		if (open) void init();
	});

	async function init() {
		bannerMessage = null;
		selected = currentOwnerId ?? '';
		try {
			const res = await apiFetch('/api/users');
			await assertOk(res, $_('project.usersLoadFailed'));
			users = await res.json();
		} catch (e: any) {
			log.error('ChangeOwnerDialog: loading users failed', e);
			bannerMessage = e.message;
		}
		await tick();
		document.getElementById(SELECT_ID)?.focus();
	}

	async function handleSave() {
		saving = true;
		bannerMessage = null;
		try {
			const res = await apiFetch(`/api/projects/${projectId}/owner`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ ownerId: selected })
			});
			await assertOk(res, $_('project.changeOwnerFailed'));
			const updated: ApiProjectDetail = await res.json();
			log.info('ChangeOwnerDialog: owner changed', { projectId, ownerId: selected });
			open = false;
			onchanged(updated);
		} catch (e: any) {
			log.error('ChangeOwnerDialog: change failed', e);
			bannerMessage = e.message;
		} finally {
			saving = false;
		}
	}

	function handleCancel() {
		open = false;
		bannerMessage = null;
	}
</script>

<svelte:window
	onkeydown={(e) => {
		if (open && e.key === 'Escape') handleCancel();
	}}
/>

{#if open}
	<div
		class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
		role="dialog"
		aria-modal="true"
		aria-label={$_('project.changeOwnerTitle')}
		data-testid="change-owner-dialog"
	>
		<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
			<h2 class="text-lg font-semibold mb-4">{$_('project.changeOwnerTitle')}</h2>

			<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

			<Field label={$_('project.changeOwnerLabel')} id={SELECT_ID} hint={$_('project.changeOwnerHint')}>
				<Select id={SELECT_ID} data-testid="change-owner-dialog.select" bind:value={selected}>
					{#if !currentOwnerId}
						<option value="">{$_('project.changeOwnerPlaceholder')}</option>
					{/if}
					{#each users as user (user.id)}
						<option value={user.id}>{user.displayName}</option>
					{/each}
				</Select>
			</Field>

			<div class="flex justify-end gap-2">
				<Button variant="secondary" data-testid="change-owner-dialog.cancel" onclick={handleCancel}>
					{$_('project.changeOwnerCancel')}
				</Button>
				<Button data-testid="change-owner-dialog.save" disabled={!canSave} onclick={handleSave}>
					{$_('project.changeOwnerSave')}
				</Button>
			</div>
		</div>
	</div>
{/if}
