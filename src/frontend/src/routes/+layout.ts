import { waitLocale } from '$lib/i18n';

export const ssr = false;

// Importing '$lib/i18n' above runs its init() side effect (register + init).
// Block first paint until the active locale's catalog is loaded, so no raw
// message key ever flashes before the dictionary is ready (the app must render
// byte-identical German — this task is a pure lift-and-shift).
export const load = async () => {
	await waitLocale();
	return {};
};
