import { waitLocale } from '$lib/i18n';
import { loadRuntimeConfig } from '$lib/config/runtimeConfig';

export const ssr = false;

// Importing '$lib/i18n' above runs its init() side effect (register + init).
// Block first paint until the active locale's catalog is loaded, so no raw
// message key ever flashes before the dictionary is ready (the app must render
// byte-identical German — this task is a pure lift-and-shift).
export const load = async () => {
	await waitLocale();
	// Runtime config (task-162) — the SPA's auth coordinates come from
	// /config.json, not from VITE_* baked in at build time. Two reasons this
	// belongs HERE and in this ORDER:
	//   * a universal `load` resolves BEFORE the layout component initialises,
	//     and therefore before every component beneath it. +layout.svelte calls
	//     getAuthProvider() at script top level, so there is no point inside it
	//     at which an await could run first. That ordering is what lets
	//     getRuntimeConfig() be written as "throws if not loaded".
	//   * the locale must be ready first: a config failure throws a message that
	//     runtimeConfig.ts resolves through get(_), and +error.svelte renders it
	//     verbatim — before waitLocale() it would render as a raw dotted key.
	// Deliberately CAUGHT, not propagated. A throw from the ROOT layout's load
	// leaves SvelteKit with no layout to render +error.svelte inside, and the
	// user gets a BLANK PAGE — fail-closed but invisible, which is not "fail
	// loudly with an error the user can see". Returning the message as data lets
	// +layout.svelte render it. loadRuntimeConfig still throws: that is its
	// contract, and getRuntimeConfig() stays unusable, so nothing downstream can
	// proceed on a missing config.
	try {
		await loadRuntimeConfig();
		return { configError: null };
	} catch (e) {
		return { configError: e instanceof Error ? e.message : String(e) };
	}
};
