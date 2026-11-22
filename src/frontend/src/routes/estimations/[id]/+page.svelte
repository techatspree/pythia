<script lang="ts">
	import Button from '#lib/ui/Button.svelte';
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { _ } from 'svelte-i18n';
	import EstimationDetail from '#lib/components/EstimationDetail.svelte';
	import VersionList from '#lib/components/VersionList.svelte';
	import ErrorBanner from '#lib/components/ErrorBanner.svelte';
	import ReplaceDraftDialog from '#lib/components/ReplaceDraftDialog.svelte';
	import MerlinStructureDialog, {
		type MerlinStructureDiff
	} from '#lib/components/MerlinStructureDialog.svelte';
	import MerlinMergeReportDialog, {
		type MerlinMergeResult
	} from '#lib/components/MerlinMergeReportDialog.svelte';
	import type { ApiEstimationDetail } from '#lib/api/types.js';
	import { apiFetch } from '#lib/api/fetch.ts';
	import { assertOk } from '#lib/api/errors.ts';
	import { base64ToBlob, downloadBlob } from '#lib/api/download.ts';
	import { log } from '#lib/log.ts';

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
	const sessionHref = $derived(estimation?.id
		? `${resolve('sessions')}?estimationId=${estimation.id}` + (estimation.projectId ? `&projectId=${estimation.projectId}` : '')
		: '');

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

	// xlsx import (task-183): the same shape as the Merlin import above — the
	// backend refuses a second draft with 409, which opens the shared
	// ReplaceDraftDialog rather than destroying the existing one.
	let xlsxInput = $state<HTMLInputElement | null>(null);
	let pendingXlsxFile = $state<File | null>(null);

	async function onXlsxFileSelected(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = '';
		if (!file) return;
		await runXlsxImport(file, false);
	}

	async function runXlsxImport(file: File, replaceDraft: boolean) {
		if (!estimation?.id) return;
		importing = true;
		bannerMessage = null;
		try {
			if (replaceDraft) {
				const del = await apiFetch(`/api/estimations/${estimation.id}/versions/draft`, {
					method: 'DELETE'
				});
				await assertOk(del, $_('estimation.importXlsxFailed'));
			}
			const form = new FormData();
			form.append('file', file);
			const res = await apiFetch(`/api/estimations/${estimation.id}/versions/import/xlsx`, {
				method: 'POST',
				body: form
			});
			if (res.status === 409 && !replaceDraft) {
				pendingXlsxFile = file;
				return;
			}
			// Two distinct failures, told apart by STATUS: 400 is "not a readable
			// workbook", 422 is "a valid workbook, but written for the other
			// estimation method". Collapsing them would leave the user unable to
			// tell a corrupt file from one that simply belongs elsewhere.
			let fallback = $_('estimation.importXlsxFailed');
			if (res.status === 400) fallback = $_('estimation.importXlsxInvalidFile');
			else if (res.status === 422) fallback = $_('estimation.importXlsxMethodMismatch');
			await assertOk(res, fallback);
			pendingXlsxFile = null;
			await loadEstimation();
		} catch (e: any) {
			log.error('importXlsx failed:', e);
			bannerMessage = e.message;
		} finally {
			importing = false;
		}
	}

	// Merge into Merlin (task-133, renamed in task-223): upload a COPY of the
	// Merlin document, get it back with this estimation's changed efforts and
	// its dependencies merged in, plus a report that opens after the download.
	// Mirrors the import flow above — the chosen File is kept in state so a 409
	// (the Merlin structure drifted) can be retried with overwriteStructure once
	// the user decides.
	let mergeInput = $state<HTMLInputElement | null>(null);
	let merging = $state(false);
	let pendingMergeFile = $state<File | null>(null);
	let structureDiff = $state<MerlinStructureDiff | null>(null);
	let mergeResult = $state<MerlinMergeResult | null>(null);

	// Which version to merge: the draft while one exists, otherwise the highest
	// submitted version. Submitting CONSUMES the draft, so hardcoding "draft"
	// would 404 exactly when the estimate is finished — the moment you actually
	// want to push it back into Merlin. Null disables the button.
	const mergeVersionRef = $derived.by<string | null>(() => {
		const versions = estimation?.versions ?? [];
		if (versions.some((v) => v.isDraft)) return 'draft';

		const latest = versions.filter((v) => !v.isDraft).reduce<number | null>((max, v) => max == null || v.versionNumber > max ? v.versionNumber : max, null);

		return latest == null ? null : String(latest);
	});

	async function onMerlinMergeFileSelected(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = '';
		if (!file) return;
		await runMerlinMerge(file, false);
	}

	async function runMerlinMerge(file: File, overwriteStructure: boolean) {
		if (!estimation?.id) return;
		const versionRef = mergeVersionRef;
		if (versionRef == null) {
			bannerMessage = $_('estimation.mergeMerlinNoVersion');
			return;
		}
		merging = true;
		bannerMessage = null;
		try {
			const form = new FormData();
			form.append('file', file);
			const res = await apiFetch(
				`/api/estimations/${estimation.id}/versions/${versionRef}/merge/merlin?overwriteStructure=${overwriteStructure}`,
				{ method: 'POST', body: form }
			);
			// The Merlin WBS no longer matches the estimation → let the user
			// decide whether to overwrite it, rather than silently rewriting.
			if (res.status === 409 && !overwriteStructure) {
				structureDiff = await res.json();
				pendingMergeFile = file;
				return;
			}
			const fallback =
				res.status === 404
					? $_('estimation.mergeMerlinNoVersion')
					: res.status === 400
						? $_('estimation.importMerlinInvalidFile')
						: $_('estimation.mergeMerlinFailed');
			await assertOk(res, fallback);
			structureDiff = null;
			pendingMergeFile = null;
			const result: MerlinMergeResult = await res.json();
			downloadBlob(base64ToBlob(result.document, 'application/octet-stream'), result.fileName);
			mergeResult = result;
		} catch (e: any) {
			log.error('mergeIntoMerlin failed:', e);
			bannerMessage = e.message;
		} finally {
			merging = false;
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
		<p class="text-ink-muted">{$_('estimation.pageLoading')}</p>
	{:else}
		<ErrorBanner
			message={bannerMessage}
			ondismiss={() => bannerMessage = null}
		/>

		{#if pendingMerlinFile}
			<ReplaceDraftDialog
				onconfirm={() => {
					const file = pendingMerlinFile;
					pendingMerlinFile = null;
					if (file) runMerlinImport(file, true);
				}}
				oncancel={() => pendingMerlinFile = null}
			/>
		{/if}
		{#if pendingXlsxFile}
			<ReplaceDraftDialog
				onconfirm={() => {
					const file = pendingXlsxFile;
					pendingXlsxFile = null;
					if (file) runXlsxImport(file, true);
				}}
				oncancel={() => pendingXlsxFile = null}
			/>
		{/if}
		{#if structureDiff}
			<MerlinStructureDialog
				diff={structureDiff}
				onconfirm={() => {
					const file = pendingMergeFile;
					structureDiff = null;
					pendingMergeFile = null;
					if (file) runMerlinMerge(file, true);
				}}
				oncancel={() => {
					structureDiff = null;
					pendingMergeFile = null;
				}}
			/>
		{/if}
		{#if mergeResult}
			<MerlinMergeReportDialog result={mergeResult} onclose={() => (mergeResult = null)} />
		{/if}
		{#if estimation}
			{#if estimation.projectId}
			<a href={resolve('/projects/[id]', { id: estimation.projectId })} class="text-sm text-brand-green hover:underline mb-4 inline-block">{$_('estimation.pageBack', { values: { project: estimation.projectName ?? $_('estimation.pageProjectFallback') } })}</a>
			{/if}
			<div class="flex items-start justify-between gap-4">
				<div class="flex-1"><EstimationDetail estimation={estimation} /></div>
				<div class="shrink-0 mt-1 flex items-center gap-2">
					<input
						type="file"
						accept=".zip,.sql,.sqlite,.mproject"
						class="hidden"
						data-testid="merlin-import-input"
						bind:this={fileInput}
						onchange={onMerlinFileSelected}
					/>
					<Button
						variant="secondary"
						data-testid="estimation-detail.import-merlin"
						onclick={() => fileInput?.click()}
						disabled={importing}
						title={$_('estimation.importMerlinHint')}
				>
					{$_('estimation.importMerlin')}
				</Button>
					<input
						type="file"
						accept=".xlsx"
						class="hidden"
						data-testid="xlsx-import-input"
						bind:this={xlsxInput}
						onchange={onXlsxFileSelected}
					/>
					<Button
						variant="secondary"
						data-testid="estimation-detail.import-xlsx"
						onclick={() => xlsxInput?.click()}
						disabled={importing}
						title={$_('estimation.importXlsxHint')}
				>
					{$_('estimation.importXlsx')}
				</Button>
					<input
						type="file"
						accept=".zip,.mproject,.sqlite,.sql"
						class="hidden"
						data-testid="merlin-export-input"
						bind:this={mergeInput}
						onchange={onMerlinMergeFileSelected}
					/>
				<Button
						variant="secondary"
						data-testid="estimation-detail.merge-merlin"
						onclick={() => mergeInput?.click()}
						disabled={merging || mergeVersionRef == null}
						title={$_('estimation.mergeMerlinHint')}
				>
					{$_('estimation.mergeMerlin')}
				</Button>
					<!-- sessionHref resolves the route via resolve(); only the dynamic
				     estimationId/projectId query is concatenated. The no-navigation-
				     without-resolve suppression this line used to carry is gone with the
				     raw <a>: the rule inspects anchor elements, not component props. -->
				<Button href={sessionHref}>
					{$_('estimation.startSession')}
				</Button>
				</div>
			</div>
		<VersionList versions={estimation.versions} estimationId={estimation.id ?? ''} oncreate={createVersion} onsubmit={submitVersion} />
		{/if}
	{/if}
</div>
