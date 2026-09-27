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
		// e2e specs drive Page Objects (`e2e/pages/`); they never reach into the
		// DOM by a TRANSLATED label, which is a moving target no compiler checks
		// (task-189). Two deliberate exemptions:
		//   - `e2e/pages/**` is WHERE selectors are supposed to live;
		//   - `e2e/language.test.ts` exists to prove 'Projekte' → 'Projects' and
		//     'Abmelden' → 'Logout' across a language switch, so asserting
		//     translated text is its entire purpose. Do not delete it as an
		//     oversight.
		// `e2e/treetable-header-fit.test.ts` also runs in both locales but needs
		// NO exemption: it is testid-driven and measures scrollWidth.
		files: ['e2e/**/*.ts'],
		ignores: ['e2e/pages/**', 'e2e/language.test.ts'],
		rules: {
			'no-restricted-syntax': [
				'error',
				// Flat config REPLACES a rule's options rather than merging them, so
				// this block would silently drop the raw-`fetch` ban for the whole
				// e2e tree if the entry were not repeated here. `e2e/global-setup.ts`
				// depends on that ban being live. Do not remove it.
				{
					selector: "CallExpression[callee.name='fetch']",
					message:
						"Use apiFetch from '$lib/api/fetch' (it attaches the Authorization header) instead of raw fetch."
				},
				{
					// getByRole(..., { name: <literal> }) — any role, not just 'button'.
					// A role-only getByRole('dialog'|'alert'|…) carries no translated
					// string and stays legal.
					selector:
						"CallExpression[callee.property.name='getByRole'] > ObjectExpression > Property[key.name='name'] > :matches(Literal, TemplateLiteral)",
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// getByText('…') with a LITERAL. getByText(<variable>) matches
					// test-generated data and stays legal.
					selector:
						"CallExpression[callee.property.name='getByText'] > :matches(Literal, TemplateLiteral):first-child",
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					selector:
						"CallExpression[callee.property.name=/^getBy(Label|Placeholder)$/] > :matches(Literal, TemplateLiteral):first-child",
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// locator('text=…'), and locator('text=' + header) — the
					// concatenated form is how one of these hid before task-188.
					selector:
						"CallExpression[callee.property.name='locator'] > Literal[value=/^\\s*text=/]:first-child, CallExpression[callee.property.name='locator'] > BinaryExpression:first-child > Literal[value=/^\\s*text=/].left",
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// { hasText: '…' } reaches the same strings by another door, in
					// locator(sel, { hasText }) and in filter({ hasText }).
					selector:
						"Property[key.name='hasText'] > :matches(Literal, TemplateLiteral)",
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				}
			]
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
