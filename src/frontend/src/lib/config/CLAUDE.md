# Runtime config (`/config.json`)

Loads when working under `src/frontend/src/lib/config/`. The frontend-wide rules are in `src/frontend/CLAUDE.md` and the root `CLAUDE.md`'s "Frontend hard rules".

## Runtime config (`/config.json`) — NOT `VITE_*` (task-162)

The SPA's auth coordinates are fetched at **runtime**, not inlined at build time. Vite inlines `VITE_*` into the bundle during `npm run build`, which made the frontend image *be* the configuration: staging and production needed separately built images and an image could not be promoted between stages. A pod environment variable cannot fix that — the bundle is already compiled, and only the browser reads what the server sends. So nginx serves `/config.json` from a Kubernetes ConfigMap and `$lib/config/runtimeConfig.ts` fetches it once.

**`VITE_*` is no longer how auth is configured.** `getAuthProvider()` reads `getRuntimeConfig().authProvider`, `EntraAuthProvider.init()` takes its four coordinates from the same place, and `.env.example` is gone — `static/config.example.json` documents the shape instead.

**It fails closed.** `getAuthProvider()` used to read `VITE_AUTH_PROVIDER ?? 'dev'`; that default is deliberately gone, because silently downgrading a misconfigured stage to the forgeable dev module is an auth downgrade. `loadRuntimeConfig()` throws on a non-OK response, a parse failure, an unknown `authProvider` (including the `"UNCONFIGURED"` sentinel the base ConfigMap carries), or an `entra` block missing any of its four fields — `redirectUri` included, which used to fall back to `http://localhost:5173` and give a deployed stage a broken login that looked configured. Mirrors the backend, where `app.auth.provider` has no unprofiled default and `AuthProviderGuard` refuses to boot dev under a NORMAL launch.

**The bootstrap seam is `+layout.ts`, not `+layout.svelte`.** A SvelteKit universal `load` resolves before the layout component initialises and therefore before every component beneath it, which is what lets `getRuntimeConfig()` be written as "throws if not loaded" — `getAuthProvider()` has eight call sites including `$lib/api/fetch.ts`. `+layout.svelte` itself calls it at script top level, so there is no point inside that file where an `await` could run first. **Order inside the load matters:** `waitLocale()` first, then `loadRuntimeConfig()`, because a config failure throws a message resolved through `get(_)` and `+error.svelte` renders `page.error?.message` verbatim — before the locale is ready that would render as a raw dotted key.

**Two ways it can fail open, both closed deliberately — do not undo either.** `nginx.conf`'s catch-all `try_files $uri $uri/ /index.html` would serve `index.html` with status **200** for a missing `/config.json`, so a `location = /config.json { try_files $uri =404; }` block sits above it. And the image build would bake a developer's locally generated config into the image — `predev` writes `static/config.json` = `{"authProvider":"dev"}` and Vite copies `static/` into `build/` — so `:frontend:imageBuild` skips `build/config.json` (task-195). Do not undo it.

Local dev, `scripts/dev.sh` and CI all get the file from **one** place: the `predev` npm script writes `static/config.json` with the dev shape, npm runs it automatically before `dev`, and `dev.sh` starts the frontend with `npm run dev`. A bare `npm run dev` therefore works too.
