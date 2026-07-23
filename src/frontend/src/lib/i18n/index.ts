import { register, init, locale, waitLocale } from 'svelte-i18n';
import { SupportedLanguage } from '$lib/domain/domain.mjs';

// Frontend i18n foundation (phase-16, task-124/125). UI strings live in the JSON
// catalogs in this folder and are rendered via svelte-i18n's `$_('key')`; no
// inline UI string literals. DE + EN catalogs are registered; `initialLocale`
// is 'de' and the root layout overrides it from the user's persisted `language`
// once the account resolves (task-125). en.json mirrors de.json key-for-key.
register('de', () => import('./de.json'));
register('en', () => import('./en.json'));

init({
	fallbackLocale: 'de',
	initialLocale: 'de'
});

/**
 * Switch the active UI language. Takes the domain's canonical `SupportedLanguage`
 * (task-111/task-123 — the one enum both sides share) and drives svelte-i18n's
 * `locale` store off its ISO `code`.
 */
export function setUserLanguage(lang: SupportedLanguage): void {
	locale.set(lang.code);
}

export { waitLocale };
