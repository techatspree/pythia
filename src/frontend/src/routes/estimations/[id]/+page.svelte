<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { _ } from 'svelte-i18n';
	import EstimationDetail from '$lib/components/EstimationDetail.svelte';
	import VersionList from '$lib/components/VersionList.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import ReplaceDraftDialog from '$lib/components/ReplaceDraftDialog.svelte';
	import MerlinStructureDialog, {
		type MerlinStructureDiff
	} from '$lib/components/MerlinStructureDialog.svelte';
	import type { ApiEstimationDetail } from '$lib/api/types.js';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { downloadResponse } from '$lib/api/download';
	import { log } from '$lib/log';

	let estimation = $state<ApiEstimationDetail | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	async function loadEstimation() {
		const id = page.params.id;
		loading = true;
		bannerMessage = null;
		try {
			const res = await apiFetch(`/api/estimations/${id}`);
			await assertOk(res, $_('estimation.pageNotFound'));
			estimation = await res.json();
		} catch (e: any) {
			log.error('loadEstimation failed:', e);
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	}

	onMount(loadEstimation);

	async function createVersion() {
		if (!estimation) return;
		try {
			const res = await apiFetch(`/api/estimations/${estimation.id ?? ''}/versions`, { method: 'POST' });
			await assertOk(res, $_('estimation.pageCreateVersionFailed'));
			await loadEstimation();
		} catch (e: any) {
			log.error('createVersion failed:', e);
			bannerMessage = e.message;
		}
	}

	// Launch a collaborative session pre-scoped to this offer. Route is resolved
	// via resolve(); the dynamic estimationId/projectId query is appended
	// separately because resolve() can only type-check a literal search suffix.
	const sessionHref = $derived(
		estimation?.id
			? `${resolve('/sessions')}?estimationId=${estimation.id}` +
				(estimation.projectId ? `&projectId=${estimation.projectId}` : '')
			: ''
	);

	// Import a Merlin project WBS as a new draft version. The backend accepts a
	// zipped .mproject bundle or its raw state.sql; we POST it as multipart
	// FormData (apiFetch leaves the body untouched so the browser sets the
	// multipart boundary itself).
	let fileInput = $state<HTMLInputElement | null>(null);
	let importing = $state(false);
	// Set when an import is blocked by an existing draft (409): holds the chosen
	// file so the user can confirm replacing the draft in one click.
	let pendingMerlinFile = $state<File | null>(null);

	async function onMerlinFileSelected(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = '';
		if (!file) return;
		await runMerlinImport(file, false);
	}

	async function runMerlinImport(file: File, replaceDraft: boolean) {
		if (!estimation?.id) return;
		importing = true;
		bannerMessage = null;
		try {
			if (replaceDraft) {
				const del = await apiFetch(`/api/estimations/${estimation.id}/versions/draft`, {
					method: 'DELETE'
				});
				await assertOk(del, $_('estimation.importMerlinFailed'));
			}
			const form = new FormData();
			form.append('file', file);
			const res = await apiFetch(`/api/estimations/${estimation.id}/versions/import/merlin`, {
				method: 'POST',
				body: form
			});
			// A draft already exists → replacing it is destructive, so open a
			// confirmation dialog (setting pendingMerlinFile) rather than acting.
			if (res.status === 409 && !replaceDraft) {
				pendingMerlinFile = file;
				return;
			}
			const fallback =
				res.status === 400
					? $_('estimation.importMerlinInvalidFile')
					: $_('estimation.importMerlinFailed');
			await assertOk(res, fallback);
			pendingMerlinFile = null;
			await loadEstimation();
		} catch (e: any) {
			log.error('importMerlin failed:', e);
			bannerMessage = e.message;
		} finally {
			importing = false;
		}
	}

	// Export (task-133): upload a COPY of the Merlin document, get it back with
	// this estimation's offerPT written into the matching activities. Mirrors
	// the import flow above — the chosen File is kept in state so a 409 (the
	// Merlin structure drifted) can be retried with overwriteStructure once the
	// user decides.
	let exportInput = $state<HTMLInputElement | null>(null);
	let exporting = $state(false);
	let pendingExportFile = $state<File | null>(null);
	let structureDiff = $state<MerlinStructureDiff | null>(null);

	// Which version to export: the draft while one exists, otherwise the highest
	// submitted version. Submitting CONSUMES the draft, so hardcoding "draft"
	// would 404 exactly when the estimate is finished — the moment you actually
	// want to push it back into Merlin. Null disables the button.
	const exportVersionRef = $derived.by<string | null>(() => {
		const versions = estimation?.versions ?? [];
		if (versions.some((v) => v.isDraft)) return 'draft';
		const latest = versions
			.filter((v) => !v.isDraft)
			.reduce<number | null>((max, v) => (max == null || v.versionNumber > max ? v.versionNumber : max), null);
		return latest == null ? null : String(latest);
	});

	async function onMerlinExportFileSelected(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = '';
		if (!file) return;
		await runMerlinExport(file, false);
	}

	async function runMerlinExport(file: File, overwriteStructure: boolean) {
		if (!estimation?.id) return;
		const versionRef = exportVersionRef;
		if (versionRef == null) {
			bannerMessage = $_('estimation.exportMerlinNoVersion');
			return;
		}
		exporting = true;
		bannerMessage = null;
		try {
			const form = new FormData();
			form.append('file', file);
			const res = await apiFetch(
				`/api/estimations/${estimation.id}/versions/${versionRef}/export/merlin?overwriteStructure=${overwriteStructure}`,
				{ method: 'POST', body: form }
			);
			// The Merlin WBS no longer matches the estimation → let the user
			// decide whether to overwrite it, rather than silently rewriting.
			if (res.status === 409 && !overwriteStructure) {
				structureDiff = await res.json();
				pendingExportFile = file;
				return;
			}
			const fallback =
				res.status === 404
					? $_('estimation.exportMerlinNoVersion')
					: res.status === 400
						? $_('estimation.importMerlinInvalidFile')
						: $_('estimation.exportMerlinFailed');
			await assertOk(res, fallback);
			structureDiff = null;
			pendingExportFile = null;
			downloadResponse(await res.blob(), res.headers.get('Content-Disposition'), 'merlin-estimated.sql');
		} catch (e: any) {
			log.error('exportMerlin failed:', e);
			bannerMessage = e.message;
		} finally {
			exporting = false;
		}
	}

	async function submitVersion() {
		if (!estimation) return;
		try {
			const res = await apiFetch(`/api/estimations/${estimation.id ?? ''}/versions/draft/submit`, { method: 'POST' });
			await assertOk(res, $_('estimation.pageSubmitFailed'));
			await loadEstimation();
		} catch (e: any) {
			log.error('submitVersion failed:', e);
			bannerMessage = e.message;
		}
	}
</script>

<div class="p-6">
	{#if loading}
		<p class="text-gray-500">{$_('estimation.pageLoading')}</p>
	{:else}
		<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		{#if pendingMerlinFile}
			<ReplaceDraftDialog
				onconfirm={() => {
					const file = pendingMerlinFile;
					pendingMerlinFile = null;
					if (file) runMerlinImport(file, true);
				}}
				oncancel={() => (pendingMerlinFile = null)}
			/>
		{/if}
		{#if structureDiff}
			<MerlinStructureDialog
				diff={structureDiff}
				onconfirm={() => {
					const file = pendingExportFile;
					structureDiff = null;
					pendingExportFile = null;
					if (file) runMerlinExport(file, true);
				}}
				oncancel={() => {
					structureDiff = null;
					pendingExportFile = null;
				}}
			/>
		{/if}
		{#if estimation}
		{#if estimation.projectId}
			<a href={resolve('/projects/[id]', { id: estimation.projectId })} class="text-sm text-brand-green hover:underline mb-4 inline-block">{$_('estimation.pageBack', { values: { project: estimation.projectName ?? $_('estimation.pageProjectFallback') } })}</a>
		{/if}
		<div class="flex items-start justify-between gap-4">
			<div class="flex-1"><EstimationDetail {estimation} /></div>
			<div class="shrink-0 mt-1 flex items-center gap-2">
				<input
					type="file"
					accept=".zip,.sql,.sqlite,.mproject"
					class="hidden"
					data-testid="merlin-import-input"
					bind:this={fileInput}
					onchange={onMerlinFileSelected}
				/>
				<button
					type="button"
					onclick={() => fileInput?.click()}
					disabled={importing}
					title={$_('estimation.importMerlinHint')}
					class="px-4 py-2 text-sm border border-brand-green text-brand-green rounded hover:bg-brand-green/10 disabled:opacity-50"
				>
					{$_('estimation.importMerlin')}
				</button>
				<input
					type="file"
					accept=".zip,.mproject,.sqlite,.sql"
					class="hidden"
					data-testid="merlin-export-input"
					bind:this={exportInput}
					onchange={onMerlinExportFileSelected}
				/>
				<button
					type="button"
					onclick={() => exportInput?.click()}
					disabled={exporting || exportVersionRef == null}
					title={$_('estimation.exportMerlinHint')}
					class="px-4 py-2 text-sm border border-brand-green text-brand-green rounded hover:bg-brand-green/10 disabled:opacity-50"
				>
					{$_('estimation.exportMerlin')}
				</button>
				<!-- sessionHref resolves the route via resolve(); only the dynamic
				     estimationId/projectId query is concatenated, which this rule cannot model. -->
				<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
				<a href={sessionHref} class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
					{$_('estimation.startSession')}
				</a>
			</div>
		</div>
		<VersionList versions={estimation.versions} estimationId={estimation.id ?? ''} oncreate={createVersion} onsubmit={submitVersion} />
		{/if}
	{/if}
</div>
