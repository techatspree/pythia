# Overview

Currently there are two modules for authentication supported:
* Dev - a static module only for development
* Entra - a dynamic modul using Entra ID

Another modules planned in the future:
* Keycloak

# Endpoint authorization

The REST API is **role-protected** — endpoints are not open. Using the
`Role` enum (`VIEWER` / `ESTIMATOR` / `ADMIN`):

* **Reads** (GET, including `export`) require **`VIEWER`** — every signed-in
  user has it (`@RolesAllowed("VIEWER")` at the resource class level).
* **Writes** (POST / PUT / DELETE — create/update project, create
  estimation, create/update/submit/delete draft, archive) require
  **`ESTIMATOR`** (`@RolesAllowed("ESTIMATOR")` on the method).
* The admin canary `GET /api/admin/ping` requires **`ADMIN`**.

An **anonymous** request → **401**; an authenticated request lacking the
required role (e.g. a `VIEWER` attempting a write) → **403**. `MeResource`
(`/api/auth/me`) keeps its own 401-on-anonymous check so it stays reachable
by any authenticated principal (it provisions the `User` on first sighting).
The `EndpointAuthorizationIT` (backend) and `endpoint-authorization.test.ts`
(Playwright, incl. a UI create that proves the frontend sends the header)
guard this contract.

# Dev auth module

The `dev` auth module is one of three concrete implementations of the
modular auth SPI declared by task-005. It uses three hard-wired
in-memory users — one per role — and a custom `Authorization: Dev
<subjectId>` header scheme. No external identity provider, no network
calls, no tokens. It is the only module needed to run the application
locally (`./scripts/dev.sh`), to execute the Playwright e2e
suite, and for outside contributors who don't have an Entra tenant.

**The `Authorization: Dev <subjectId>` header is forgeable** — it carries no
signature or token, so anyone can claim any subject/role. It is therefore
blocked outside dev/test, fail-closed: `app.auth.provider` has no default
value (a profile that fails to set it will not start), and `AuthProviderGuard`
refuses to boot when the `dev` provider is active under a `NORMAL` (production)
launch. Production uses the `entra` module with signed OIDC Bearer tokens
validated by `quarkus-oidc`.

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

## Strict mode (no fallback)

The dev module is **strict**: there is no default-user fallback. A request
must carry a valid `Authorization: Dev <subjectId>` header (one of the
hard-wired ids) or it is rejected with **401**. A missing/blank header is
anonymous → 401; a malformed header, wrong scheme (`Bearer`, lowercase
`dev`), or unknown subject is also 401. This holds for every profile that
runs the dev module (`%dev`, `%test`), so authz paths are exercised the same
way locally as under Entra.

Because the dev backend on :8080 enforces this itself, the
`e2e/auth-gate.test.ts` cases run directly against it — no separate strict
backend, and no environment switch needed to enable them.

## Automated coverage — dev module

`src/frontend/e2e/auth.test.ts` exercises the canary endpoint
`/api/admin/ping` (gated by `@RolesAllowed("ADMIN")`) and the
provider-agnostic `/api/auth/me` endpoint against the **dev**
backend (port 8080), which runs the strict dev module. Five API-level
cases, all using Playwright's `request` fixture (no browser navigation):

| #  | Case                                                                                              | Expectation                                                 |
|----|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| 1  | `GET /api/auth/me` with NO `Authorization` header                                                 | 401 (strict — no default-user fallback)                     |
| 2  | `GET /api/auth/me` with `Authorization: Dev dev-viewer`                                           | 200 + `subjectId: "dev-viewer"`, `roles: ["VIEWER"]`        |
| 3  | `GET /api/auth/me` with `Authorization: Dev nope-not-a-user`                                      | 401 (filter rejects unknown subject)                        |
| 4  | `GET /api/admin/ping` with `Authorization: Dev dev-viewer`                                        | 403 (authenticated but lacks `ADMIN`)                        |
| 5  | `GET /api/admin/ping` with `Authorization: Dev dev-admin`                                         | 200 + `{message: "pong", user: "dev-admin"}`                 |

Case 4 specifically proves the augmentor cleanup from task-006: the
`DevSecurityIdentityAugmentor` now populates a real
`QuarkusSecurityIdentity` with role strings, so `@RolesAllowed("ADMIN")`
can distinguish `dev-viewer` (403) from `dev-admin` (200).

`e2e/auth-gate.test.ts` adds three API-level cases proving the strict
rejection (no header, unknown `Dev` subject, and a `Bearer` token) plus
the SPA dev-login-dialog gate, all against the dev backend on :8080.

Run the full suite (dev backend on :8080 must be up):

```bash
cd src/frontend && npx playwright test
```

Expected count: **27 passing** (smoke 13 + tree-table 5 + auth-gate 4 +
auth 5); all run against the dev backend on :8080, Docker up.


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
