// The dev server's half of the "Open-Source-Lizenzen" dialog (task-210), used by
// the `pythia-oss-notices-dev` plugin in vite.config.ts. Plain JavaScript on
// purpose: it needs Node's fs, and the frontend's type-check has no Node typings.
//
// Serves exactly GET/HEAD /third-party-notices.json and /THIRD-PARTY-LICENSES.txt
// from <dir> (the repository root's build/oss-notices/, written by a full
// `./gradlew build`), read per request so a build while dev.sh runs is picked
// up. Without a copy it answers 404 itself, rather than SvelteKit's index.html
// fallback, so the dialog sees a clean "not available yet". Every other request
// goes to next().

import { readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const FILES = {
	'/third-party-notices.json': 'application/json; charset=utf-8',
	'/THIRD-PARTY-LICENSES.txt': 'text/plain; charset=utf-8'
};

/**
 * @param {import('vite').ViteDevServer} server
 * @param {URL} dirUrl the directory holding the two files
 */
export function serveOssNotices(server, dirUrl) {
	const dir = fileURLToPath(dirUrl);
	const notices = join(dir, 'third-party-notices.json');
	let mtime = null;
	try {
		mtime = statSync(notices).mtime;
	} catch {
		// no full build yet; reported below
	}
	server.config.logger.info(
		mtime
			? `pythia-oss-notices-dev: serving ${notices} (built ${mtime.toISOString()})`
			: `pythia-oss-notices-dev: no ${notices} yet; the licences dialog shows "not available" until ./gradlew build`
	);
	server.middlewares.use((req, res, next) => {
		const path = (req.url ?? '').split('?')[0];
		const type = FILES[path];
		if (!type || (req.method !== 'GET' && req.method !== 'HEAD')) return next();
		let body;
		try {
			body = readFileSync(join(dir, path.slice(1)));
		} catch {
			res.statusCode = 404;
			res.setHeader('Cache-Control', 'no-store');
			res.end();
			return;
		}
		res.statusCode = 200;
		res.setHeader('Content-Type', type);
		res.setHeader('Cache-Control', 'no-store');
		res.end(req.method === 'HEAD' ? undefined : body);
	});
}
