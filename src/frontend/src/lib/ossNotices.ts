import { apiFetch } from '#lib/api/fetch.ts';
import { assertOk } from '#lib/api/errors.ts';
import { log } from '#lib/log.ts';

// The "Open-Source-Lizenzen" dialog's data (task-210): /third-party-notices.json,
// generated at build time from the SBOM by scripts/generate-oss-notices.mjs and
// served as a static file (in dev by vite.config.ts' pythia-oss-notices-dev).

export interface OssNoticeComponent {
	name: string;
	licenses: string[];
	copyright: string[];
	authors: string[];
	homepage: string | null;
}

export interface OssNoticePart {
	part: 'frontend' | 'backend';
	components: OssNoticeComponent[];
}

export interface OssNotices {
	parts: OssNoticePart[];
}

/** Case-insensitive substring filter over name, licenses, copyright lines and authors. */
export function filterComponents(components: OssNoticeComponent[], query: string): OssNoticeComponent[] {
	const q = query.trim().toLowerCase();
	if (!q) return components;
	return components.filter((c) =>
		[c.name, ...c.licenses, ...c.copyright, ...c.authors].some((s) => s.toLowerCase().includes(q))
	);
}

// Fetched once per page load; reopening the dialog does not refetch.
let cached: OssNotices | null = null;

/**
 * The notices, or 'unavailable' when the file does not exist (404: dev mode
 * without a full build). Any other failure throws, with `loadFailed` as the
 * message for a non-OK status.
 */
export async function loadOssNotices(loadFailed: string): Promise<OssNotices | 'unavailable'> {
	if (cached) return cached;
	const res = await apiFetch('/third-party-notices.json');
	if (res.status === 404) {
		log.info('ossNotices: /third-party-notices.json not available (no full build yet)');
		return 'unavailable';
	}
	await assertOk(res, loadFailed);
	const notices = (await res.json()) as OssNotices;
	const count = (part: string) => notices.parts.find((p) => p.part === part)?.components.length ?? 0;
	log.info('ossNotices: loaded', { frontend: count('frontend'), backend: count('backend') });
	cached = notices;
	return notices;
}
