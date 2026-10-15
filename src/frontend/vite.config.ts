import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';
import sbom from 'rollup-plugin-sbom';

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit(),
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
		}
	],
	server: {
		proxy: {
			'/api': 'http://localhost:8090',
			// WebSocket upgrades for the collaborative-session channel (task-066).
			'/ws': { target: 'http://localhost:8090', ws: true }
		}
	}
});
