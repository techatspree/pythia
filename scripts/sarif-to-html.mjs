// Dependency-free consolidator for the project's static-analysis reports.
// Reads detekt SARIF files and ESLint JSON reports, writes one merged SARIF
// plus one self-contained HTML overview. Node built-ins only — run through the
// gradle-node plugin's managed Node (no host Node / Python / Docker).
//
// Usage:
//   node sarif-to-html.mjs --out <html> --sarif <mergedSarif>
//        [--detekt <file.sarif> ...] [--eslint <eslint.json> ...]

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname } from 'node:path';

function parseArgs(argv) {
	const opts = { out: null, sarif: null, detekt: [], eslint: [] };
	for (let i = 0; i < argv.length; i++) {
		const a = argv[i];
		if (a === '--out') opts.out = argv[++i];
		else if (a === '--sarif') opts.sarif = argv[++i];
		else if (a === '--detekt') opts.detekt.push(argv[++i]);
		else if (a === '--eslint') opts.eslint.push(argv[++i]);
	}
	return opts;
}

function readJson(path) {
	if (!path || !existsSync(path)) {
		process.stderr.write(`sarif-to-html: skipping missing report ${path}\n`);
		return null;
	}
	try {
		const text = readFileSync(path, 'utf8').trim();
		return text ? JSON.parse(text) : null;
	} catch (e) {
		process.stderr.write(`sarif-to-html: skipping unparseable report ${path}: ${e.message}\n`);
		return null;
	}
}

// A detekt SARIF file contributes its runs verbatim.
function detektRuns(sarif) {
	return Array.isArray(sarif?.runs) ? sarif.runs : [];
}

// An ESLint JSON report is normalised into a single SARIF run.
function eslintRun(eslint) {
	if (!Array.isArray(eslint)) return null;
	const results = [];
	for (const file of eslint) {
		for (const m of file.messages ?? []) {
			results.push({
				ruleId: m.ruleId ?? 'eslint',
				level: m.severity === 2 ? 'error' : 'warning',
				message: { text: m.message ?? '' },
				locations: [
					{
						physicalLocation: {
							artifactLocation: { uri: file.filePath ?? '' },
							region: { startLine: m.line ?? 0 }
						}
					}
				]
			});
		}
	}
	return { tool: { driver: { name: 'ESLint' } }, results };
}

// Flatten SARIF runs into rows for the HTML table.
function rowsFromRun(run) {
	const tool = run?.tool?.driver?.name ?? 'unknown';
	return (run?.results ?? []).map((r) => {
		const loc = r.locations?.[0]?.physicalLocation;
		return {
			tool,
			rule: r.ruleId ?? '',
			level: r.level ?? 'warning',
			file: loc?.artifactLocation?.uri ?? '',
			line: loc?.region?.startLine ?? '',
			message: r.message?.text ?? ''
		};
	});
}

function esc(v) {
	return String(v).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}

function renderHtml(rows) {
	const byTool = {};
	for (const r of rows) {
		byTool[r.tool] ??= { error: 0, warning: 0, other: 0 };
		const b = byTool[r.tool];
		if (r.level === 'error') b.error++;
		else if (r.level === 'warning') b.warning++;
		else b.other++;
	}
	const summary = Object.entries(byTool)
		.map(([t, c]) => `<tr><td>${esc(t)}</td><td>${c.error}</td><td>${c.warning}</td><td>${c.other}</td></tr>`)
		.join('');
	const body = rows.length
		? rows
				.map(
					(r) =>
						`<tr class="${esc(r.level)}"><td>${esc(r.tool)}</td><td>${esc(r.rule)}</td>` +
						`<td>${esc(r.level)}</td><td>${esc(r.file)}</td><td>${esc(r.line)}</td>` +
						`<td>${esc(r.message)}</td></tr>`
				)
				.join('')
		: '<tr><td colspan="6">No findings.</td></tr>';
	return `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<title>Static analysis — consolidated report</title>
<style>
 body{font-family:system-ui,sans-serif;margin:2rem;color:#1f2937}
 h1{font-size:1.4rem} h2{font-size:1.1rem;margin-top:2rem}
 table{border-collapse:collapse;width:100%;font-size:.85rem}
 th,td{border:1px solid #e5e7eb;padding:.35rem .5rem;text-align:left;vertical-align:top}
 th{background:#f3f4f6}
 tr.error td{background:#fef2f2} tr.warning td{background:#fffbeb}
 td:nth-child(5){text-align:right;font-variant-numeric:tabular-nums}
</style></head><body>
<h1>Static analysis — consolidated report</h1>
<p>${rows.length} finding(s) across detekt (Kotlin) and ESLint (frontend). Generated ${new Date().toISOString()}.</p>
<h2>Summary</h2>
<table><thead><tr><th>Tool</th><th>Errors</th><th>Warnings</th><th>Other</th></tr></thead><tbody>${summary}</tbody></table>
<h2>Findings</h2>
<table><thead><tr><th>Tool</th><th>Rule</th><th>Level</th><th>File</th><th>Line</th><th>Message</th></tr></thead><tbody>${body}</tbody></table>
</body></html>
`;
}

function main() {
	const opts = parseArgs(process.argv.slice(2));
	const runs = [];
	for (const p of opts.detekt) {
		const s = readJson(p);
		if (s) runs.push(...detektRuns(s));
	}
	for (const p of opts.eslint) {
		const s = readJson(p);
		const run = s && eslintRun(s);
		if (run) runs.push(run);
	}

	const rows = runs.flatMap(rowsFromRun);

	if (opts.sarif) {
		mkdirSync(dirname(opts.sarif), { recursive: true });
		writeFileSync(
			opts.sarif,
			JSON.stringify(
				{ version: '2.1.0', $schema: 'https://json.schemastore.org/sarif-2.1.0.json', runs },
				null,
				2
			)
		);
	}
	if (opts.out) {
		mkdirSync(dirname(opts.out), { recursive: true });
		writeFileSync(opts.out, renderHtml(rows));
	}
	process.stderr.write(`sarif-to-html: ${rows.length} finding(s) from ${runs.length} run(s)\n`);
}

main();
