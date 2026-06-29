# Overview

Currently there are two modules for authentication supported:
* Dev - a static module only for development
* Entra - a dynamic modul using Entra ID

Another modules planned in the future:
* Keycloak

# Dev auth module

The `dev` auth module is one of three concrete implementations of the
modular auth SPI declared by task-005. It uses three hard-wired
in-memory users — one per role — and a custom `Authorization: Dev
<subjectId>` header scheme. No external identity provider, no network
calls, no tokens. It is the only module needed to run the application
locally (`./scripts/dev.sh`), to execute the Playwright e2e
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
| `%dev`         | `dev-admin`            | local Quarkus dev mode — `curl` smoke calls keep working   |
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
./scripts/dev-strict.sh
```

That script starts the same `dev` Quarkus profile on port
**8081** with `APP_AUTH_DEV_DEFAULT_USER=` (empty) exported, so the
filter rejects any request that lacks a valid `Authorization: Dev
<subjectId>` header. Playwright auth-gate cases pick this backend up
via the `STRICT_BACKEND_URL` env var:

```bash
./scripts/dev-strict.sh &                        # background
STRICT_BACKEND_URL=http://localhost:8081 \
    npx playwright test e2e/auth-gate.test.ts
```

When `STRICT_BACKEND_URL` is unset, the three strict-backend cases
self-skip, so the default `npx playwright test` invocation against
the dev backend on :8080 still works.

## Automated coverage — dev module

`src/frontend/e2e/auth.test.ts` exercises the canary endpoint
`/api/admin/ping` (gated by `@RolesAllowed("ADMIN")`) and the
provider-agnostic `/api/auth/me` endpoint against the **dev**
backend (port 8080) where the `app.auth.dev.default-user=dev-admin`
fallback is active. Five API-level cases, all using Playwright's
`request` fixture (no browser navigation):

| #  | Case                                                                                              | Expectation                                                 |
|----|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| 1  | `GET /api/auth/me` with NO `Authorization` header                                                 | 200 + `subjectId: "dev-admin"`, `providerName: "dev"` — falls back to the default-user |
| 2  | `GET /api/auth/me` with `Authorization: Dev dev-viewer`                                           | 200 + `subjectId: "dev-viewer"`, `roles: ["VIEWER"]`        |
| 3  | `GET /api/auth/me` with `Authorization: Dev nope-not-a-user`                                      | 401 (filter rejects unknown subject)                        |
| 4  | `GET /api/admin/ping` with `Authorization: Dev dev-viewer`                                        | 403 (authenticated but lacks `ADMIN`)                        |
| 5  | `GET /api/admin/ping` with `Authorization: Dev dev-admin`                                         | 200 + `{message: "pong", user: "dev-admin"}`                 |

Case 4 specifically proves the augmentor cleanup from task-006: the
`DevSecurityIdentityAugmentor` now populates a real
`QuarkusSecurityIdentity` with role strings, so `@RolesAllowed("ADMIN")`
can distinguish `dev-viewer` (403) from `dev-admin` (200).

The **strict-mode** dev backend (`scripts/dev-strict.sh` on port
8081, `APP_AUTH_DEV_DEFAULT_USER=` empty) is covered by the
`auth-gate.test.ts` cases from task-006, which self-skip when
`STRICT_BACKEND_URL` is unset.

Run the full suite (dev backend on :8080 must be up):

```bash
cd src/frontend && npx playwright test
```

Expected count: **24 passing + 3 skipped** (smoke 13 + tree-table 5 +
auth-gate 1 always-on + auth 5; 3 strict-backend cases skip unless
`STRICT_BACKEND_URL=http://localhost:8081` is exported).


## What NOT to do

- **Do NOT deploy the dev module to production.** The per-profile
  defaults guard against this (`%prod.app.auth.provider=entra`), but
  the rule stands: `APP_AUTH_PROVIDER=dev` outside of local
  development is a misconfiguration.
- **Do NOT** issue a header shaped like `Bearer <jwt>` from the dev
  provider. The format is reserved for the Entra/Keycloak modules
  and the dev filter rejects it explicitly so mistakes surface as
  401s, not silent role assignments.

# Entra auth module

TODO: add documentation

## Manual checklist for verification

Playwright cannot exercise the Entra module without an Entra tenant
and pre-assigned test users (see `docs/entra-setup.md` for the
tenant-side prerequisites). Walk through the list below against a real
tenant; each box gets a date + initials when verified.

Prerequisites:
- A working `entra` deployment (minikube via `./scripts/minikube-deploy.sh`
  OR local via the env-override workflow in `docs/entra-setup.md`).
- Three Entra test users assigned to the `VIEWER`, `ESTIMATOR`, and
  `ADMIN` app roles on `estimation-api`.

Checklist:

- [ ] Log in via the SPA as the VIEWER user. `GET /api/auth/me`
  returns `200` with `roles: ["VIEWER"]` and `providerName: "entra"`.
  _Date / initials: ______________________________________________

- [ ] As the same VIEWER user, `GET /api/admin/ping` returns
  `403` (authenticated but lacks `ADMIN`).
  _Date / initials: ______________________________________________

- [ ] Log out, log in as the ESTIMATOR user. `GET /api/admin/ping`
  still returns `403` (ESTIMATOR ⊂ VIEWER+ESTIMATOR, no ADMIN).
  _Date / initials: ______________________________________________

- [ ] Log out, log in as the ADMIN user. `GET /api/admin/ping`
  returns `200` with `{message: "pong", user: "<oid>"}`.
  _Date / initials: ______________________________________________

- [ ] After ~1 hour with the SPA tab open, MSAL's silent token
  acquire refreshes successfully and the next
  `GET /api/admin/ping` (still as ADMIN) returns `200` WITHOUT
  a fresh interactive login.
  _Date / initials: ______________________________________________

- [ ] Logout via the SPA. `GET /api/auth/me` (no Bearer token)
  returns `401` (the MeResource guard kicks in because the
  EntraAuthFilter sees `isAnonymous == true` and leaves
  `CurrentUserProvider.current` null).
  _Date / initials: ______________________________________________

Any failed checkbox should be filed as a bug against the auth modules
before phase-2 is considered closed.
