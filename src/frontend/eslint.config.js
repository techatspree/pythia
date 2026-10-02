import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import svelte from 'eslint-plugin-svelte';
import svelteParser from 'svelte-eslint-parser';
import htmlPlugin from '@html-eslint/eslint-plugin';
import htmlParser from '@html-eslint/parser';
import globals from 'globals';

// e2e translated-label ban, attribute forms (task-191). Written with String.raw
// so the regexes are not double-escaped inside the esquery selector strings.
//
// An attribute selector on a translatable attribute whose value starts with a
// literal character. Two cases stay legal: the four English aria-labels of the
// deliberately un-internationalised `treetable/TreeTable.svelte`, and a value
// interpolated from a template literal (`[title="${catalogText(…)}"]`), whose
// quasi ENDS at the opening quote. That is why the lookaheads read `(?!\2|$)`
// and `(?![\s"']|$)`: a bare `(?!\2)` succeeds at end of string and would flag
// every catalogText() interpolation.
const TRANSLATABLE_ATTR_SELECTOR = String.raw`/\[(title|placeholder|aria-label|alt)[*^$~|]?=\s*(?:(["'])(?!\2|$)(?!(?:Children|Root nodes|Collapse|Expand)\2)|(?![\s"']|$)(?!(?:Children|Root nodes|Collapse|Expand)\]))/`;
const TRANSLATABLE_ATTR_NAME = String.raw`/^(aria-label|title|placeholder|alt)$/`;
const TREETABLE_LABEL = String.raw`/^(Children|Root nodes|Collapse|Expand)$/`;

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
					// getByTitle/getByAltText are the API doors to the same `title`
					// and `alt` attributes the selector entries below ban (task-191);
					// leaving them open would just move the next leftover there.
					selector:
						"CallExpression[callee.property.name=/^getBy(Label|Placeholder|Title|AltText)$/] > :matches(Literal, TemplateLiteral):first-child",
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
				},
				{
					// A translated value inside a CSS attribute selector
					// (`[title="Ziehen zum Sortieren"]`, `input[placeholder="Gruppentitel…"]`)
					// — two of task-190's English-run leftovers, invisible to every
					// entry above because they use no banned API. Matched ANYWHERE,
					// not only as a locator() argument: specs build selectors in
					// constants and inside page.evaluate strings.
					selector: `Literal[value=${TRANSLATABLE_ATTR_SELECTOR}]`,
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// The same, in the literal parts of a template literal.
					selector: `TemplateElement[value.cooked=${TRANSLATABLE_ATTR_SELECTOR}]`,
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// toHaveAttribute('aria-label', /kritischen Kette/) — the other two
					// task-190 leftovers. A string or regex literal on a translatable
					// attribute; the four TreeTable labels stay legal, and so does a
					// catalogText()/catalogPattern() call (not a Literal).
					selector: `CallExpression[callee.property.name='toHaveAttribute'][arguments.0.value=${TRANSLATABLE_ATTR_NAME}] > Literal:nth-child(2):not([value=${TREETABLE_LABEL}])`,
					message:
						'e2e specs must go through Page Objects (see src/frontend/e2e/pages/); do not match on a translated label.'
				},
				{
					// The same with a plain template literal; one that interpolates
					// (a catalog lookup, test data) stays legal.
					selector: `CallExpression[callee.property.name='toHaveAttribute'][arguments.0.value=${TRANSLATABLE_ATTR_NAME}] > TemplateLiteral:nth-child(2)[expressions.length=0]`,
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
