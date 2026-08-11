<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import { _ } from 'svelte-i18n';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';

	let { open = $bindable(false), oncreated }: { open: boolean; oncreated: () => void } = $props();

	let name = $state('');
	let description = $state('');
	let client = $state('');
	let loading = $state(false);
	let bannerMessage = $state<string | null>(null);

	async function handleSubmit() {
		if (!name.trim()) {
			bannerMessage = $_('dialog.createProject.nameRequired');
			return;
		}
		loading = true;
		bannerMessage = null;
		try {
			const res = await apiFetch('/api/projects', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ name: name.trim(), description: description.trim() || null, client: client.trim() || null })
			});
			await assertOk(res, $_('dialog.createProject.createFailed'));
			name = '';
			description = '';
			client = '';
			open = false;
			oncreated();
		} catch (e: any) {
			log.error('CreateProjectDialog: create failed', e);
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	}

	function handleCancel() {
		open = false;
		bannerMessage = null;
	}
</script>

{#if open}
	<div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" role="dialog">
		<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
			<h2 class="text-lg font-semibold mb-4">{$_('dialog.createProject.title')}</h2>

			<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

			<form onsubmit={handleSubmit}>
				<div class="mb-3">
					<label class="block text-sm font-medium mb-1" for="name">{$_('dialog.createProject.nameLabel')}</label>
					<input id="name" bind:value={name} class="w-full border rounded px-3 py-2 text-sm" required />
				</div>
				<div class="mb-3">
					<label class="block text-sm font-medium mb-1" for="description">{$_('dialog.createProject.descriptionLabel')}</label>
					<input id="description" bind:value={description} class="w-full border rounded px-3 py-2 text-sm" />
				</div>
				<div class="mb-4">
					<label class="block text-sm font-medium mb-1" for="client">{$_('dialog.createProject.clientLabel')}</label>
					<input id="client" bind:value={client} class="w-full border rounded px-3 py-2 text-sm" />
				</div>
				<div class="flex justify-end gap-2">
					<Button variant="secondary" onclick={handleCancel}>{$_('dialog.createProject.cancel')}</Button>
					<Button type="submit" disabled={loading}>
						{loading ? $_('dialog.createProject.creating') : $_('dialog.createProject.create')}
					</Button>
				</div>
			</form>
		</div>
	</div>
{/if}
