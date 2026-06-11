# Entra ID auth module

The `entra` auth module is the production-grade concrete implementation
of the modular auth SPI declared by task-005. Activation is purely
config-driven: set `APP_AUTH_PROVIDER=entra` on the backend and
`VITE_AUTH_PROVIDER=entra` on the frontend; everything else flows
through the SPI. For local development the `dev` module
(see `docs/auth-dev.md`) stays the default; this document covers
production and the optional "developer-Entra" workflow.

## One-time tenant setup (manual, performed by the tenant admin)

Two app registrations under the same Entra tenant:

1. **`estimation-api`** — the backend.
   - Set the **Application ID URI** to `api://<api-client-id>`.
   - Expose a custom scope **`access`**; full identifier
     `api://<api-client-id>/access`.
   - Define three **app roles** with values matching the domain
     `Role` enum exactly:
     - `VIEWER`
     - `ESTIMATOR`
     - `ADMIN`
   - Allowed member types: **Users/Groups** (so test users can be
     assigned to the roles via the Enterprise Application page).

2. **`estimation-spa`** — the SvelteKit SPA.
   - Platform type: **Single-page application**.
   - Redirect URIs:
     - `http://localhost:5173` (local Entra mode + smoke runs)
     - `https://estimation.<your-domain>` (production)
   - **API permissions** → Add → My APIs → `estimation-api` →
     Delegated `access`. Grant admin consent.

3. Assign at least one **test user per role** to `estimation-api` via
   the tenant's *Enterprise Applications → estimation-api → Users
   and groups* page. task-008's manual verification checklist
   requires one VIEWER, one ESTIMATOR, and one ADMIN test user.

4. Record three identifiers in a secure place (NEVER commit):
   - `ENTRA_TENANT_ID` — the tenant GUID.
   - `ENTRA_API_CLIENT_ID` — `estimation-api`'s Application (client) ID.
   - `ENTRA_SPA_CLIENT_ID` — `estimation-spa`'s Application (client) ID.

## Backend wiring

The `entra` module is implemented in
`src/backend/implementation/src/main/kotlin/io/github/theestimator/auth/entra/`:

- `EntraAuthModule` — declares `name() = "entra"`,
  `@IfBuildProperty`-gated to `app.auth.provider=entra`.
- `EntraAuthFilter` — post-matching `ContainerRequestFilter` at
  `Priorities.AUTHENTICATION + 100`. Quarkus's OIDC layer has already
  validated the access token by the time this filter runs; the filter
  reads claims via the injected request-scoped `JsonWebToken` plus the
  `SecurityIdentity` principal, maps Entra app-role values into the
  domain `Role` enum, and writes the resulting `CurrentUser` into
  the request-scoped `CurrentUserProvider`. Anonymous identities are
  passed through (no `current` is written; the resource sees a missing
  `CurrentUser` and Quarkus rejects the call at the OIDC layer if a
  token is required).
- `EntraSecurityIdentityAugmentor` — pure claim → role-string mapping.
  It does NOT inject or write to `CurrentUserProvider` (that crashes on
  the Vert.x event loop, per the task-006 lesson). The augmentor
  enriches the input `SecurityIdentity` with the matching Quarkus role
  strings (`VIEWER` / `ESTIMATOR` / `ADMIN`) so `@RolesAllowed`
  declarations work uniformly across providers.
- `EntraRoleMapper` — shared `entraRolesToDomain(...)` helper, called
  by both the filter and the augmentor.

The Quarkus OIDC config is wired per profile in
`src/backend/implementation/src/main/resources/application.properties`.
Under `%prod` and `%dev-minikube`:

```
quarkus.oidc.auth-server-url=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0
quarkus.oidc.client-id=${ENTRA_API_CLIENT_ID}
quarkus.oidc.application-type=service
quarkus.oidc.token.audience=api://${ENTRA_API_CLIENT_ID}
quarkus.oidc.roles.role-claim-path=roles
```

`%dev-local`, `%dev`, and `%test` keep `quarkus.oidc.enabled=false`
because the dev module covers those paths.

## Frontend wiring

The MSAL-based provider lives in
`src/frontend/src/lib/auth/EntraAuthProvider.ts` and uses
`@azure/msal-browser` (NOT msal-react — this project is Svelte 5).
Flow:

- `init()` → instantiate `PublicClientApplication` with
  `clientId=VITE_ENTRA_SPA_CLIENT_ID`,
  `authority=https://login.microsoftonline.com/<tenant>`, the redirect
  URI from env, and `cache.cacheLocation: 'localStorage'`. Calls
  `msal.initialize()` and `handleRedirectPromise()` so the post-login
  redirect is processed.
- `login()` → `msal.loginRedirect({ scopes: ['api://<api-client-id>/access'] })`.
  Redirect (not popup) for robustness against popup blockers.
- `logout()` → `msal.logoutRedirect()`.
- `getAccount()` → maps the active account's id-token claims into the
  provider-agnostic `AuthAccount` shape (subjectId from
  `oid`/`sub`, email from `email`/`preferred_username`,
  displayName from `name`, roles from the `roles` claim — only
  recognised values pass through the filter).
- `getAuthorizationHeader()` → `msal.acquireTokenSilent` for the API
  scope; on `InteractionRequiredAuthError` falls back to
  `acquireTokenRedirect`. Returns `"Bearer <accessToken>"`.

## Environment variables

| variable                     | side     | description                                            |
|------------------------------|----------|--------------------------------------------------------|
| `APP_AUTH_PROVIDER`          | backend  | `entra` to activate this module                        |
| `ENTRA_TENANT_ID`            | backend  | resolves `${ENTRA_TENANT_ID}` in application.properties|
| `ENTRA_API_CLIENT_ID`        | backend  | API app's client id; drives `client-id` + audience     |
| `VITE_AUTH_PROVIDER`         | frontend | `entra` to activate this module                        |
| `VITE_ENTRA_TENANT_ID`       | frontend | mirrors the backend tenant id                          |
| `VITE_ENTRA_SPA_CLIENT_ID`   | frontend | SPA app's client id (MSAL `clientId`)                  |
| `VITE_ENTRA_API_CLIENT_ID`   | frontend | API app's client id (used to derive the access scope)  |
| `VITE_ENTRA_REDIRECT_URI`    | frontend | redirect URI (default `http://localhost:5173`)         |

`ENTRA_SPA_CLIENT_ID` is also enforced by `scripts/minikube-deploy.sh`
as a precheck so deploys cannot silently ship unresolved placeholders.

## Running Entra locally (optional)

To test the Entra wiring against a real tenant from a local checkout
(no minikube), stop `dev-local.sh`, export the eight variables above,
and start Quarkus + Vite manually:

```bash
export APP_AUTH_PROVIDER=entra
export ENTRA_TENANT_ID=<tenant-guid>
export ENTRA_API_CLIENT_ID=<api-client-id>
export ENTRA_SPA_CLIENT_ID=<spa-client-id>
export VITE_AUTH_PROVIDER=entra
export VITE_ENTRA_TENANT_ID="$ENTRA_TENANT_ID"
export VITE_ENTRA_SPA_CLIENT_ID="$ENTRA_SPA_CLIENT_ID"
export VITE_ENTRA_API_CLIENT_ID="$ENTRA_API_CLIENT_ID"

cd src/backend/implementation && mvn quarkus:dev -Pdev-local
# in another shell:
cd src/frontend && npm run dev
```

`http://localhost:5173` must be a registered redirect URI on
`estimation-spa` for this to work.

## How env vars flow into the minikube backend Pod

`scripts/minikube-deploy.sh` exports `APP_AUTH_PROVIDER=entra`,
`VITE_AUTH_PROVIDER=entra`, and prechecks the three `ENTRA_*` ids before
invoking `mvn package -Pdev-minikube` and `kubectl apply -k`. The Pod
inherits these env vars via the existing Deployment manifest's `env:`
forwarding (no manifest edits required); Quarkus resolves
`${ENTRA_TENANT_ID}` etc. in `application.properties` at startup from
process env. If you operate a non-minikube cluster, set the same env
vars on the backend Deployment via your cluster's Secret / ConfigMap
plumbing and the substitution still happens at runtime.

## Cross-references

- Modular SPI: `src/backend/implementation/src/main/kotlin/io/github/theestimator/auth/`
  + `src/frontend/src/lib/auth/`.
- Dev module (default for local development): `docs/auth-dev.md`.
- Keycloak module (planned): `task-060`.
