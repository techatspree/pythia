import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, type Plugin } from 'vite';
import sbom from 'rollup-plugin-sbom';
import { serveOssNotices } from './scripts/oss-notices-dev.mjs';

// The "Open-Source-Lizenzen" dialog's two files in DEV mode (task-210). A full
// `./gradlew build` generates them into the repository root's build/oss-notices/
// (production serves them from the SPA); the dev server serves static/, not
// build/, so this plugin answers exactly those two URLs from that persistent
// copy, which may be older than the working tree's dependencies.
// Resolved from this file's location, never from process.cwd().
const OSS_NOTICES_DIR = new URL('../../build/oss-notices/', import.meta.url);

function ossNoticesDev(): Plugin {
	return {
		name: 'pythia-oss-notices-dev',
		apply: 'serve',
		configureServer(server) {
			serveOssNotices(server, OSS_NOTICES_DIR);
		}
	};
}

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit({
			compilerOptions: {
				runes: ({ filename }) => filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},
			adapter: adapter({ fallback: 'index.html' })
		}),

		// The npm part of the frontend SBOM (task-206): exactly the packages
		// bundled into the SPA. Production builds only. It writes into the build
		// output, and SvelteKit copies that into build/cyclonedx/, the directory
		// the image is made from. :frontend:removeBundledSbom deletes it again,
		// or nginx would serve every dependency version publicly.
		// scripts/sbom-finalize.mjs reads the client copy from .svelte-kit/output/client/.
		{
			...sbom({
				specVersion: '1.6',
				outFormats: ['json'],
				saveTimestamp: false,
				generateSerial: false
			}),
			apply: 'build'
		},

		// Dev only (task-210): serves build/oss-notices/ at the repository root.
		ossNoticesDev()
	],
	server: {
		proxy: {
			'/api': 'http://localhost:8090',
			// WebSocket upgrades for the collaborative-session channel (task-066).
			'/ws': { target: 'http://localhost:8090', ws: true }
		}
	}
});
