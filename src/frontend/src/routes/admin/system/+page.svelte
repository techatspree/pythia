<script lang="ts">
	import { onMount } from 'svelte';
	import { _, locale } from 'svelte-i18n';
	import RequiredRole from '$lib/auth/RequiredRole.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import { formatDate, DEFAULT_LOCALE } from '$lib/format';
	import {
		getSystemSettings,
		updateSystemSettings,
		getStandardDrivers,
		replaceStandardDrivers,
		uploadSystemCss,
		deleteSystemCss,
		type EffortDriverDto,
		type SystemSettingsDto
	} from '$lib/api/system';
	import { system } from '$lib/stores/system.svelte';
	import { log } from '$lib/log';

	// Admin surface for the per-installation settings (task-146): the display
	// name, the standard effort-driver template, and the custom stylesheet.
	let settings = $state<SystemSettingsDto | null>(null);
	let displayName = $state('');
	let drivers = $state<EffortDriverDto[]>([]);
	let bannerMessage = $state<string | null>(null);
	let notice = $state<string | null>(null);
	let cssInput = $state<HTMLInputElement | null>(null);

	const loc = $derived($locale ?? DEFAULT_LOCALE);

	async function load() {
		try {
			settings = await getSystemSettings();
			displayName = settings.displayName ?? '';
			drivers = await getStandardDrivers();
		} catch (e: unknown) {
			log.error('admin/system: load failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	onMount(load);

	async function saveName() {
		notice = null;
		try {
			const trimmed = displayName.trim();
			await updateSystemSettings(trimmed === '' ? null : trimmed);
			settings = await getSystemSettings();
			system.apply(settings);
			notice = $_('admin.system.nameSaved');
		} catch (e: unknown) {
			log.error('admin/system: saving the display name failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	async function saveDrivers() {
		notice = null;
		try {
			drivers = await replaceStandardDrivers(
				drivers.map((d) => ({
					description: d.description,
					factor: d.factor,
					comment: d.comment ?? ''
				}))
			);
			notice = $_('admin.system.driversSaved');
		} catch (e: unknown) {
			log.error('admin/system: saving the standard drivers failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}

	function addDriver() {
		drivers.push({ description: '', factor: 0, comment: '' });
	}

	function removeDriver(index: number) {
		drivers.splice(index, 1);
	}

	function move(index: number, delta: number) {
		const target = index + delta;
		if (target < 0 || target >= drivers.length) return;
		const [row] = drivers.splice(index, 1);
		drivers.splice(target, 0, row);
	}

	async function onCssSelected(event: Event) {
		notice = null;
		const input = event.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		try {
			await uploadSystemCss(file);
			notice = $_('admin.system.cssUploaded');
			// The stylesheet is a <link> in the document head, so it only changes
			// on a fresh document load.
			window.location.reload();
		} catch (e: unknown) {
			log.error('admin/system: uploading the stylesheet failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
			input.value = '';
		}
	}

	async function removeCss() {
		notice = null;
		try {
			await deleteSystemCss();
			notice = $_('admin.system.cssRemoved');
			window.location.reload();
		} catch (e: unknown) {
			log.error('admin/system: removing the stylesheet failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}
</script>

<div class="p-6 max-w-3xl mx-auto">
	<h1 class="text-2xl font-bold mb-4">{$_('admin.system.title')}</h1>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
	{#if notice}
		<p data-testid="system-notice" class="mb-4 text-sm text-brand-green">{notice}</p>
	{/if}

	<RequiredRole role="ADMIN">
		{#if !settings}
			<!-- The form is gated on the loaded settings on purpose: rendering the
			     inputs first shows an empty name field for a configured
			     installation, and anything typed in that window is overwritten the
			     moment the load resolves (or worse, saved over the real name). -->
			<p data-testid="system-loading" class="text-gray-500">{$_('admin.system.loading')}</p>
		{:else}
		<section class="mb-8 border rounded-lg p-4 bg-white">
			<h2 class="text-lg font-semibold mb-1">{$_('admin.system.nameSection')}</h2>
			<p class="text-sm text-gray-500 mb-3">{$_('admin.system.nameHint')}</p>
			<label class="block text-sm mb-1" for="system-display-name"
				>{$_('admin.system.nameLabel')}</label
			>
			<input
				id="system-display-name"
				data-testid="system-display-name"
				bind:value={displayName}
				class="border rounded px-2 py-1 w-full max-w-sm mb-3"
			/>
			<div>
				<button
					type="button"
					data-testid="system-save-name"
					onclick={saveName}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
				>
					{$_('admin.system.nameSave')}
				</button>
			</div>
		</section>

		<section class="mb-8 border rounded-lg p-4 bg-white">
			<h2 class="text-lg font-semibold mb-1">{$_('admin.system.driversSection')}</h2>
			<p class="text-sm text-gray-500 mb-3">{$_('admin.system.driversHint')}</p>

			{#if drivers.length === 0}
				<p class="text-sm text-gray-500 mb-3">{$_('admin.system.driversEmpty')}</p>
			{:else}
				<table class="w-full text-sm mb-3">
					<thead class="text-xs text-gray-500">
						<tr>
							<th class="text-left py-1">{$_('admin.system.driverDescription')}</th>
							<th class="text-right py-1 w-24">{$_('admin.system.driverFactor')}</th>
							<th class="text-left py-1">{$_('admin.system.driverComment')}</th>
							<th class="w-32"></th>
						</tr>
					</thead>
					<tbody>
						{#each drivers as driver, i (i)}
							<tr data-testid="system-driver-row">
								<td class="py-1 pr-2">
									<input bind:value={driver.description} class="border rounded px-2 py-1 w-full" />
								</td>
								<td class="py-1 pr-2">
									<input
										type="number"
										step="0.01"
										bind:value={driver.factor}
										class="border rounded px-2 py-1 w-full text-right"
									/>
								</td>
								<td class="py-1 pr-2">
									<input bind:value={driver.comment} class="border rounded px-2 py-1 w-full" />
								</td>
								<td class="py-1 text-right whitespace-nowrap">
									<button
										type="button"
										title={$_('admin.system.driverUp')}
										onclick={() => move(i, -1)}
										class="px-1 text-gray-500 hover:text-gray-700">↑</button
									>
									<button
										type="button"
										title={$_('admin.system.driverDown')}
										onclick={() => move(i, 1)}
										class="px-1 text-gray-500 hover:text-gray-700">↓</button
									>
									<button
										type="button"
										title={$_('admin.system.driverRemove')}
										onclick={() => removeDriver(i)}
										class="px-1 text-gray-500 hover:text-red-600">✕</button
									>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}

			<div class="flex items-center gap-3">
				<button
					type="button"
					data-testid="system-add-driver"
					onclick={addDriver}
					class="px-3 py-1.5 text-sm border rounded hover:bg-gray-50"
				>
					{$_('admin.system.driverAdd')}
				</button>
				<button
					type="button"
					data-testid="system-save-drivers"
					onclick={saveDrivers}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
				>
					{$_('admin.system.driversSave')}
				</button>
			</div>
		</section>

		<section class="border rounded-lg p-4 bg-white">
			<h2 class="text-lg font-semibold mb-1">{$_('admin.system.cssSection')}</h2>
			<p class="text-sm text-gray-500 mb-3">{$_('admin.system.cssHint')}</p>

			{#if settings?.hasCustomCss}
				<p data-testid="system-css-current" class="text-sm text-gray-700 mb-3">
					{$_('admin.system.cssCurrent', {
						values: {
							filename: settings.customCssFilename ?? '—',
							updated: settings.customCssUpdatedAt
								? formatDate(settings.customCssUpdatedAt, loc)
								: '—'
						}
					})}
				</p>
			{:else}
				<p data-testid="system-css-none" class="text-sm text-gray-500 mb-3">
					{$_('admin.system.cssNone')}
				</p>
			{/if}

			<div class="flex items-center gap-3">
				<label class="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 cursor-pointer">
					{$_('admin.system.cssUpload')}
					<input
						bind:this={cssInput}
						data-testid="system-css-input"
						type="file"
						accept=".css,text/css"
						onchange={onCssSelected}
						class="hidden"
					/>
				</label>
				{#if settings?.hasCustomCss}
					<button
						type="button"
						data-testid="system-css-remove"
						onclick={removeCss}
						class="px-3 py-1.5 text-sm text-gray-500 hover:text-red-600"
					>
						{$_('admin.system.cssRemove')}
					</button>
				{/if}
			</div>
		</section>
		{/if}
	</RequiredRole>
</div>
