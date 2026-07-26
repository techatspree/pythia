import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	server: {
		proxy: {
			'/api': 'http://localhost:8080',
			// WebSocket upgrades for the collaborative-session channel (task-066).
			'/ws': { target: 'http://localhost:8080', ws: true }
		}
	}
});
