<script lang="ts">
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { apiFetch } from '$lib/api/fetch';
	import { log } from '$lib/log';
	import { availableMethods } from '$lib/methods/registry';
	import type { components } from '$lib/api/schema';

	type EstimationSummaryDto = components['schemas']['EstimationSummaryDto'];
	type EstimationMethod = components['schemas']['EstimationMethod'];

	let {
		open = $bindable(false),
		projectId,
		oncreated
	}: {
		open: boolean;
		projectId: string;
		oncreated: (created: EstimationSummaryDto) => void;
	} = $props();

	let offer = $state('');
	let description = $state('');
	let method = $state<EstimationMethod>('THREE_POINT_PERT');
	let loading = $state(false);
	let bannerMessage = $state<string | null>(null);

	async function handleSubmit() {
		if (!offer.trim()) {
			bannerMessage = 'Angebot ist erforderlich';
			return;
		}
		loading = true;
		bannerMessage = null;
		log.debug('CreateEstimationDialog: POST /api/projects/', projectId, '/estimations', 'method', method);
		try {
			const res = await apiFetch(`/api/projects/${projectId}/estimations`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					offer: offer.trim(),
					description: description.trim() || null,
					method
				})
			});
			if (!res.ok) {
				let message = res.statusText;
				try {
					const body = await res.json();
					if (body && typeof body.message === 'string') message = body.message;
				} catch {
					/* non-JSON error body — keep statusText */
				}
				if (res.status === 403) {
					message = 'Sie sind nicht berechtigt, eine Kalkulation anzulegen.';
				}
				log.error(
					'CreateEstimationDialog: create failed with status',
					res.status,
					message
				);
				bannerMessage = message;
				return;
			}
			const created = (await res.json()) as EstimationSummaryDto;
			offer = '';
			description = '';
			method = 'THREE_POINT_PERT';
			open = false;
			oncreated(created);
		} catch (e: unknown) {
			const message = e instanceof Error ? e.message : String(e);
			log.error('CreateEstimationDialog: network error', message);
			bannerMessage = message;
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
			<h2 class="text-lg font-semibold mb-4">New Offer</h2>

			<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

			<form onsubmit={handleSubmit}>
				<div class="mb-3">
					<label class="block text-sm font-medium mb-1" for="offer">Angebot *</label>
					<input
						id="offer"
						bind:value={offer}
						class="w-full border rounded px-3 py-2 text-sm"
						required
					/>
				</div>
				<div class="mb-3">
					<label class="block text-sm font-medium mb-1" for="method">Methode</label>
					<select
						id="method"
						bind:value={method}
						class="w-full border rounded px-3 py-2 text-sm"
					>
						{#each availableMethods as m (m.method)}
							<option value={m.method}>{m.label}</option>
						{/each}
					</select>
				</div>
				<div class="mb-4">
					<label class="block text-sm font-medium mb-1" for="description">Beschreibung</label>
					<textarea
						id="description"
						bind:value={description}
						rows="3"
						class="w-full border rounded px-3 py-2 text-sm"
					></textarea>
				</div>
				<div class="flex justify-end gap-2">
					<button
						type="button"
						onclick={handleCancel}
						class="px-4 py-2 text-sm border rounded hover:bg-gray-50">Abbrechen</button
					>
					<button
						type="submit"
						disabled={loading}
						class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
					>
						{loading ? 'Speichern…' : 'Speichern'}
					</button>
				</div>
			</form>
		</div>
	</div>
{/if}
