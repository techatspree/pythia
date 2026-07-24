<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { setUserLanguage } from '$lib/i18n';
	import { SupportedLanguage } from '$lib/domain/domain.mjs';
	import { apiFetch } from '$lib/api/fetch';
	import { assertOk } from '$lib/api/errors';
	import { log } from '$lib/log';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { getAuthProvider } from '$lib/auth';

	const ROLE_ORDER = [
		'VIEWER',
		'ESTIMATOR',
		'ADMIN'
	] as const;

	let {
		account,
		onlogout
	}: {
		account: AuthAccount;
		onlogout: () => void;
	} = $props();

	let bannerMessage = $state<string | null>(null);

	// Persist the chosen language to the backend, then switch the active locale.
	// Maps the ISO code to the domain's canonical SupportedLanguage (fromCode is
	// not JS-exported, so look it up by code) which setUserLanguage expects.
	async function changeLanguage(code: string) {
		const lang = SupportedLanguage.values().find((v) => v.code === code);
		if (!lang || code === $locale) return;
		try {
			const res = await apiFetch('/api/auth/me/language', {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ language: code })
			});
			await assertOk(res, $_('language.switchFailed'));
			setUserLanguage(lang);
		} catch (e: unknown) {
			log.error('UserMenu: language switch failed', e);
			bannerMessage = e instanceof Error ? e.message : String(e);
		}
	}
</script>

<div class="relative flex items-center gap-3">
	<span class="text-sm text-gray-700">
		{account.displayName ?? account.subjectId}
	</span>
	<div class="flex items-center gap-1">
		{#each ROLE_ORDER as r (r)}
			{#if account.roles.includes(r)}
				<span
					class="text-xs uppercase tracking-wide px-1.5 py-0.5 rounded bg-brand-green/10 text-brand-green"
				>{r}</span>
			{/if}
		{/each}
	</div>
	<select
		class="text-sm border rounded px-1 py-0.5 bg-white text-gray-700"
		data-testid="language-select"
		aria-label={$_('language.label')}
		value={$locale ?? 'de'}
		onchange={(e) => changeLanguage(e.currentTarget.value)}
	>
		<option value="de">{$_('language.de')}</option>
		<option value="en">{$_('language.en')}</option>
	</select>
	<button
		type="button"
		data-testid="logout-button"
		class="text-sm text-brand-green hover:text-[#007a45] underline-offset-2 hover:underline"
		onclick={async () => {
			await getAuthProvider().logout();
			onlogout();
		}}
	>{$_('menu.user.logout')}</button>

	{#if bannerMessage}
		<div class="absolute right-0 top-full mt-2 w-72 z-50">
			<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />
		</div>
	{/if}
</div>
