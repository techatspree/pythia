import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import svelte from 'eslint-plugin-svelte';
import svelteParser from 'svelte-eslint-parser';
import htmlPlugin from '@html-eslint/eslint-plugin';
import htmlParser from '@html-eslint/parser';
import globals from 'globals';

export default [
	{
		ignores: [
			'.svelte-kit/',
			'build/',
			'node/',
			'node_modules/',
			'src/lib/domain/',
			'src/lib/api/schema.d.ts',
			'reports/',
			'test-results/'
		]
	},

	js.configs.recommended,

	...tseslint.configs.recommended.map((c) => ({
		...c,
		files: ['**/*.ts']
	})),

	...svelte.configs['flat/recommended'].map((c) => ({
		...c,
		files: c.files ?? ['**/*.svelte'],
		languageOptions: {
			...c.languageOptions,
			parser: svelteParser,
			parserOptions: {
				...(c.languageOptions?.parserOptions ?? {}),
				parser: tseslint.parser,
				extraFileExtensions: ['.svelte']
			},
			globals: {
				...(c.languageOptions?.globals ?? {}),
				...globals.browser
			}
		}
	})),

	{
		// Svelte <script lang="ts"> blocks are parsed by the TS parser, so the
		// TS-aware unused-vars rule must lint them — the base no-unused-vars rule
		// mis-flags TS function-type parameter names and ignores the project's
		// `_`-prefixed "intentionally unused" convention used in snippet params.
		files: ['**/*.svelte'],
		plugins: { '@typescript-eslint': tseslint.plugin },
		rules: {
			'no-unused-vars': 'off',
			'@typescript-eslint/no-unused-vars': [
				'error',
				{
					argsIgnorePattern: '^_',
					varsIgnorePattern: '^_',
					caughtErrorsIgnorePattern: '^_'
				}
			]
		}
	},

	{
		// Ban raw `fetch(...)`: every `/api` call must go through `apiFetch`
		// (`$lib/api/fetch.ts`), which attaches the Authorization header.
		files: ['**/*.ts', '**/*.svelte'],
		rules: {
			'no-restricted-syntax': [
				'error',
				{
					selector: "CallExpression[callee.name='fetch']",
					message:
						"Use apiFetch from '$lib/api/fetch' (it attaches the Authorization header) instead of raw fetch."
				}
			]
		}
	},

	{
		// The one legitimate raw-fetch site: apiFetch wraps the real `fetch`.
		files: ['src/lib/api/fetch.ts'],
		rules: {
			'no-restricted-syntax': 'off'
		}
	},

	{
		// Build-time Node scripts (e.g. the icon generator): they run under Node,
		// not the browser, so `console` / `Buffer` and friends must be in scope.
		files: ['scripts/**/*.mjs'],
		languageOptions: {
			globals: { ...globals.node }
		}
	},

	{
		...htmlPlugin.configs['flat/recommended'],
		files: ['**/*.html'],
		languageOptions: {
			...(htmlPlugin.configs['flat/recommended'].languageOptions ?? {}),
			parser: htmlParser
		}
	}
];
