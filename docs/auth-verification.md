# Auth verification

Phase-2 closes with this document. It captures both the **automated**
coverage (the dev module is fully exercised by Playwright) and the
**manual** Entra verification checklist (which requires a real tenant
+ assigned test users that CI doesn't have access to).

## Automated coverage — dev module

`src/frontend/e2e/auth.test.ts` exercises the canary endpoint
`/api/admin/ping` (gated by `@RolesAllowed("ADMIN")`) and the
provider-agnostic `/api/auth/me` endpoint against the **dev-local**
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

The **strict-mode** dev backend (`scripts/dev-local-strict.sh` on port
8081, `APP_AUTH_DEV_DEFAULT_USER=` empty) is covered by the
`auth-gate.test.ts` cases from task-006, which self-skip when
`STRICT_BACKEND_URL` is unset.

Run the full suite (dev-local backend on :8080 must be up):

```bash
cd src/frontend && npx playwright test
```

Expected count: **24 passing + 3 skipped** (smoke 13 + tree-table 5 +
auth-gate 1 always-on + auth 5; 3 strict-backend cases skip unless
`STRICT_BACKEND_URL=http://localhost:8081` is exported).

## Manual checklist — Entra module

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
