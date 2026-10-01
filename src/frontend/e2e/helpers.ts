import type { Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

const SUPPORTED_LANGS = ['de', 'en'] as const;
export type E2eLang = (typeof SUPPORTED_LANGS)[number];

function resolveE2eLang(raw: string): E2eLang {
	if (!(SUPPORTED_LANGS as readonly string[]).includes(raw)) {
		throw new Error(
			`E2E_LANG=${raw} is not a supported catalog (expected one of: ${SUPPORTED_LANGS.join(', ')})`
		);
	}
	return raw as E2eLang;
}

/**
 * The run's UI language (task-190) — the ONE source for it. `global-setup.ts`
 * pins the shared dev users to it, and `catalogText`/`parseNumber` and the
 * language specs' restore steps read it, so none of them can disagree.
 * `E2E_LANG=en npx playwright test` is a real English run.
 */
export const E2E_LANG: E2eLang = resolveE2eLang(process.env.E2E_LANG ?? 'de');

let catalog: Record<string, unknown> | undefined;

/**
 * The exact string a catalog key renders to in the run's language. Used instead
 * of a hardcoded sentence so an assertion pins the KEY the UI chose, not its
 * wording.
 */
export function catalogText(key: string): string {
	catalog ??= JSON.parse(
		readFileSync(new URL(`../src/lib/i18n/${E2E_LANG}.json`, import.meta.url), 'utf-8')
	) as Record<string, unknown>;
	const value = key
		.split('.')
		.reduce<unknown>((node, part) => (node as Record<string, unknown>)?.[part], catalog);
	if (typeof value !== 'string') throw new Error(`i18n key not found: ${key}`);
	return value;
}

/**
 * `catalogText(key)` as an anchored pattern, with every ICU `{placeholder}`
 * matching any text — for a translated template whose values a spec does not
 * pin (e.g. an aria-label carrying a title and a date).
 */
export function catalogPattern(key: string): RegExp {
	const escaped = catalogText(key)
		.split(/\{[^}]+\}/)
		.map((part) => part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
	return new RegExp(`^${escaped.join('.+')}$`);
}

/**
 * Parses a number the app rendered in the run's language. The separators come
 * from `Intl` rather than being hardcoded; this is test-side PARSING of rendered
 * output, not a second formatter (and `$lib/format.ts` is unreachable here).
 */
export function parseNumber(text: string): number {
	const parts = new Intl.NumberFormat(E2E_LANG).formatToParts(1234.5);
	const group = parts.find((p) => p.type === 'group')?.value ?? '';
	const decimal = parts.find((p) => p.type === 'decimal')?.value ?? '.';
	const normalised = text.trim().split(group).join('').split(decimal).join('.');
	return parseFloat(normalised);
}

export async function loginAsDev(page: Page, subjectId: string): Promise<void> {
	await page.addInitScript(
		([k, v]) => {
			localStorage.setItem(k, v);
		},
		['devAuthSubject', subjectId]
	);
}

export async function logoutDev(page: Page): Promise<void> {
	await page.addInitScript((k) => {
		localStorage.removeItem(k);
	}, 'devAuthSubject');
}
