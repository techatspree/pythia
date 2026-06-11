# Dev auth module

The `dev` auth module is one of three concrete implementations of the
modular auth SPI declared by task-005 (the others are `entra` for Entra
ID and `keycloak` planned for task-060). It uses three hard-wired
in-memory users — one per role — and a custom `Authorization: Dev
<subjectId>` header scheme. No external identity provider, no network
calls, no tokens. It is the only module needed to run the application
locally (`./scripts/dev-local.sh`), to execute the Playwright e2e
suite, and for outside contributors who don't have an Entra tenant.

## Hard-wired users

| subjectId       | display name   | roles                              |
|-----------------|----------------|------------------------------------|
| `dev-viewer`    | Dev Viewer     | `VIEWER`                           |
| `dev-estimator` | Dev Estimator  | `VIEWER`, `ESTIMATOR`              |
| `dev-admin`     | Dev Admin      | `VIEWER`, `ESTIMATOR`, `ADMIN`     |

The frontend picker (`DevLoginDialog.svelte`) shows these three as
buttons. Selection is stored under `localStorage.devAuthSubject` so
it survives reloads (and Playwright's `storageState` snapshots).
Logout clears the key.

## Wire format

REST calls send `Authorization: Dev <subjectId>`. Examples:

```
Authorization: Dev dev-admin
Authorization: Dev dev-viewer
```

The backend filter (`DevAuthFilter`) parses the header as:

- Split on the FIRST whitespace.
- First token MUST be exactly `Dev` (case-sensitive — `dev` / `DEV` /
  `Bearer` all reject with **401**).
- `subjectId` MUST match `^[a-z][a-z0-9-]*$` AND be one of the three
  hard-wired ids; mismatches → **401**.

## Default-user fallback (developer convenience)

The backend property `app.auth.dev.default-user` controls what happens
when no `Authorization` header is sent. Per-profile defaults:

| profile        | default                | rationale                                                 |
|----------------|------------------------|-----------------------------------------------------------|
| *(file-level)* | *(empty)* — strict     | safe default; explicit profiles opt in                    |
| `%dev-local`   | `dev-admin`            | developer convenience — `curl` smoke calls keep working    |
| `%dev`         | `dev-admin`            | Quarkus dev mode                                          |
| `%test`        | `dev-admin`            | keeps the existing `@QuarkusTest` IT suite green          |
| `%dev-minikube`| *(empty)* — strict     | minikube deployments use Entra; dev module is not active  |
| `%prod`        | *(empty)* — strict     | never use the dev module in prod, but be safe if misconfigured |

To override at runtime (e.g. in CI), set the env var
`APP_AUTH_DEV_DEFAULT_USER=` (empty) to force strict mode regardless
of the profile.

## Strict-mode integration (the second backend)

To prove the dev module rejects unauthenticated access — and so the
new `e2e/auth-gate.test.ts` cases can run alongside the existing 18
smoke + tree-table cases — we run a **second** backend instance:

```bash
./scripts/dev-local-strict.sh
```

That script starts the same `dev-local` Quarkus profile on port
**8081** with `APP_AUTH_DEV_DEFAULT_USER=` (empty) exported, so the
filter rejects any request that lacks a valid `Authorization: Dev
<subjectId>` header. Playwright auth-gate cases pick this backend up
via the `STRICT_BACKEND_URL` env var:

```bash
./scripts/dev-local-strict.sh &                  # background
STRICT_BACKEND_URL=http://localhost:8081 \
    npx playwright test e2e/auth-gate.test.ts
```

When `STRICT_BACKEND_URL` is unset, the three strict-backend cases
self-skip, so the default `npx playwright test` invocation against
the dev-local backend on :8080 still works.

## What NOT to do

- **Do NOT deploy the dev module to production.** The per-profile
  defaults guard against this (`%prod.app.auth.provider=entra`), but
  the rule stands: `APP_AUTH_PROVIDER=dev` outside of local
  development is a misconfiguration.
- **Do NOT** issue a header shaped like `Bearer <jwt>` from the dev
  provider. The format is reserved for the Entra/Keycloak modules
  and the dev filter rejects it explicitly so mistakes surface as
  401s, not silent role assignments.
