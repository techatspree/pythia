<script lang="ts">
	import Badge from '#lib/ui/Badge.svelte';
	import Button from '#lib/ui/Button.svelte';
	import Field from '#lib/ui/Field.svelte';
	import { tick } from 'svelte';
	import { _, locale } from 'svelte-i18n';
	import ErrorBanner from '#lib/components/ErrorBanner.svelte';
	import { log } from '#lib/log.ts';
	import { DEFAULT_LOCALE, formatNumber } from '#lib/format.ts';
	import { filterComponents, loadOssNotices, type OssNotices } from '#lib/ossNotices.ts';

	// The "Open-Source-Lizenzen" dialog (task-210): every shipped third-party
	// component with its licences and copyright holders, from the build-time
	// /third-party-notices.json (generated from the SBOM). Copyright lines are
	// legal text and shown verbatim; the product name only via `brand.name`.
	let { open = $bindable(false) }: { open: boolean } = $props();

	const FILTER_ID = 'oss-notices-filter';

	let notices = $state<OssNotices | null>(null);
	let unavailable = $state(false);
	let bannerMessage = $state<string | null>(null);
	let filter = $state('');

	let sections = $derived(
		(notices?.parts ?? []).map((p) => ({ part: p.part, components: filterComponents(p.components, filter) }))
	);
	let noMatch = $derived(notices !== null && sections.every((s) => s.components.length === 0));

	$effect(() => {
		if (open) void init();
	});

	async function init() {
		bannerMessage = null;
		filter = '';
		try {
			const result = await loadOssNotices($_('ossNotices.loadFailed'));
			unavailable = result === 'unavailable';
			notices = result === 'unavailable' ? null : result;
		} catch (e: unknown) {
			log.error('OssNoticesDialog: loading failed', e);
			notices = null;
			bannerMessage = e instanceof Error ? e.message : String(e);
			return;
		}
		await tick();
		document.getElementById(FILTER_ID)?.focus();
	}

	function close() {
		open = false;
		bannerMessage = null;
	}

	const count = (n: number) => formatNumber(n, $locale ?? DEFAULT_LOCALE);
</script>

<svelte:window
	onkeydown={(e) => {
		if (open && e.key === 'Escape') close();
	}}
/>

{#if open}
	<div
		class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
		role="dialog"
		aria-modal="true"
		aria-label={$_('ossNotices.title')}
		data-testid="oss-notices-dialog"
	>
		<div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-3xl max-h-[85vh] flex flex-col">
			<h2 class="text-lg font-semibold mb-4">{$_('ossNotices.title')}</h2>

			<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

			<p class="text-sm text-ink-body mb-4" data-testid="oss-notices-dialog.intro">
				{$_('ossNotices.intro', { values: { name: $_('brand.name') } })}
			</p>

			{#if unavailable}
				<p class="text-sm text-ink-muted mb-4" data-testid="oss-notices-dialog.unavailable">
					{$_('ossNotices.unavailable')}
				</p>
			{:else if notices}
				<Field label={$_('ossNotices.filterLabel')} id={FILTER_ID}>
					<input
						id={FILTER_ID}
						type="search"
						data-testid="oss-notices-dialog.filter"
						placeholder={$_('ossNotices.filterPlaceholder')}
						bind:value={filter}
						class="w-full border rounded px-3 py-2 text-sm"
					/>
				</Field>

				<div class="min-h-0 flex-1 overflow-y-auto mb-4">
					{#if noMatch}
						<p class="text-sm text-ink-muted" data-testid="oss-notices-dialog.no-match">
							{$_('ossNotices.noMatch', { values: { filter: filter.trim() } })}
						</p>
					{/if}
					{#each sections as section (section.part)}
						{#if section.components.length > 0}
							<section class="mb-4" data-testid="oss-notices-dialog.section.{section.part}">
								<h3 class="text-sm font-semibold text-ink-strong mb-2" data-testid="oss-notices-dialog.part.{section.part}">
									{$_(`ossNotices.part.${section.part}`, { values: { count: count(section.components.length) } })}
								</h3>
								<ul class="border border-hairline rounded divide-y divide-hairline">
									{#each section.components as c (c.name)}
										<li class="px-3 py-2 text-sm" data-testid="oss-notices-dialog.component" data-name={c.name}>
											<div class="flex items-start justify-between gap-2">
												<span class="text-ink-strong font-medium break-all">{c.name}</span>
												<span class="flex flex-wrap justify-end gap-1">
													{#each c.licenses as license (license)}
														<Badge>{license}</Badge>
													{/each}
												</span>
											</div>
											{#if c.copyright.length > 0}
												{#each c.copyright as line, i (i)}
													<div class="text-xs text-ink-muted pl-3">{line}</div>
												{/each}
											{:else if c.authors.length > 0}
												<div class="text-xs text-ink-muted pl-3">
													{$_('ossNotices.authors', { values: { names: c.authors.join(', ') } })}
												</div>
											{:else}
												<div class="text-xs text-ink-muted pl-3">{$_('ossNotices.copyrightUnknown')}</div>
											{/if}
											{#if c.homepage}
												<div class="text-xs pl-3">
													<a
														href={c.homepage}
														target="_blank"
														rel="noopener noreferrer"
														class="text-brand-green hover:underline"
													>{$_('ossNotices.homepage')} <span aria-hidden="true">↗</span></a>
												</div>
											{/if}
										</li>
									{/each}
								</ul>
							</section>
						{/if}
					{/each}
				</div>
			{/if}

			<div class="flex items-center justify-between gap-2">
				<span>
					{#if notices}
						<a
							href="/THIRD-PARTY-LICENSES.txt"
							target="_blank"
							rel="noopener noreferrer"
							class="text-sm text-brand-green hover:underline"
							data-testid="oss-notices-dialog.full-text"
						>{$_('ossNotices.fullText')} <span aria-hidden="true">↗</span></a>
					{/if}
				</span>
				<Button variant="secondary" data-testid="oss-notices-dialog.close" onclick={close}>
					{$_('ossNotices.close')}
				</Button>
			</div>
		</div>
	</div>
{/if}
