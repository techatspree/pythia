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
   - **Set the accepted access-token version to v2.0.** In the app
     registration **Manifest**, set `"accessTokenAcceptedVersion": 2`
     (Graph app-manifest format: `"api": { "requestedAccessTokenVersion":
     2 }`). This is **required**: the backend uses the v2.0 authority
     (`quarkus.oidc.auth-server-url=…/v2.0`) and therefore expects the
     issuer `https://login.microsoftonline.com/<tenant>/v2.0`. By default
     Azure issues **v1.0** access tokens for an API (issuer
     `https://sts.windows.net/<tenant>/`), even when the SPA logs in via
     the v2.0 endpoint — so without this setting every call fails token
     validation with:
     `Issuer (iss) claim value (https://sts.windows.net/<tenant>/) doesn't
     match expected value of https://login.microsoftonline.com/<tenant>/v2.0`
     (visible as a 401 on the frontend, and in the backend
     `io.quarkus.oidc` DEBUG log). After changing it, users must
     re-acquire a token (log out / clear the SPA's `localStorage`) since
     the old v1.0 token is cached.
   - Define three **app roles** whose **`value`** matches the domain
     `Role` enum EXACTLY:
     - `VIEWER`
     - `ESTIMATOR`
     - `ADMIN`

     The `value` field is what lands in the token's `roles` claim. The
     mapper (`EntraRoleMapper.entraRolesToDomain`) is case-insensitive but
     **silently drops any unrecognised value** — so a typo or a different
     naming convention (e.g. `Viewer.All`) leaves the user with **no role**:
     they authenticate fine but every write returns 403. (Display name and
     description are free-form; only `value` matters.)
   - Allowed member types: **Users/Groups** (so test users can be
     assigned to the roles via the Enterprise Application page).
   - Defining a role does **not** grant it. Assign users under
     *Enterprise Applications → estimation-api → Users and groups* (see
     step 3). Role/manifest changes only take effect in a **new** token, so
     the user must log out / clear the SPA's `localStorage` afterward.

2. **`estimation-spa`** — the SvelteKit SPA.
   - Platform type: **Single-page application**. This is critical:
     browser MSAL redeems the auth code for a token via a *cross-origin*
     (CORS) call, which Entra permits ONLY for redirect URIs registered
     under the **Single-page application** platform. Registering the same
     URI under the **Web** platform instead yields, at token redemption:
     `AADSTS9002326: Cross-origin token redemption is permitted only for
     the 'Single-Page Application' client-type`. If you hit that, remove
     the URI from the Web platform and re-add it under Single-page
     application.
   - Redirect URIs (all under the Single-page application platform):
     - `http://localhost:5173` (local Entra mode + smoke runs)
     - `http://localhost:8080` (minikube — the default port-forward URL
       `minikube-deploy.sh` bakes in as `VITE_ENTRA_REDIRECT_URI`; if you
       access the SPA via the Ingress host or a different port, register
       and export that URL instead)
     - `https://estimation.<your-domain>` (production)
   - **API permissions** → Add → My APIs → `estimation-api` →
     Delegated `access`. Grant admin consent.
   - **App roles for the SPA UI (recommended).** The frontend reads roles
     from the **ID token** (issued for THIS app), so role-gated UI
     (`RequiredRole.svelte`, the user menu) only sees roles that are defined
     as app roles on `estimation-spa` **and assigned to the user here**.
     Backend enforcement does NOT need this (it uses the access token →
     `estimation-api` roles), but without it the UI shows the user as having
     no roles even though the API accepts their writes. Define the same
     `VIEWER`/`ESTIMATOR`/`ADMIN` app roles here and assign the same users.
     See [Roles: enforcement vs. UI display](#roles-enforcement-vs-ui-display).

3. Assign at least one **test user per role** to `estimation-api` via
   the tenant's *Enterprise Applications → estimation-api → Users
   and groups* page. task-008's manual verification checklist
   requires one VIEWER, one ESTIMATOR, and one ADMIN test user.

4. Record three identifiers in a secure place (NEVER commit):
   - `ENTRA_TENANT_ID` — the tenant GUID.
   - `ENTRA_API_CLIENT_ID` — `estimation-api`'s Application (client) ID.
   - `ENTRA_SPA_CLIENT_ID` — `estimation-spa`'s Application (client) ID.

## Roles: enforcement vs. UI display

Roles live in **two** places because two different tokens carry them, and
app roles only appear in a token whose **audience is the app they are
defined on**:

- **Backend enforcement** (`@RolesAllowed`, the 401/403 decision) reads the
  **access token**, whose audience is `estimation-api`. So authorization is
  driven by app roles defined **and assigned** on **`estimation-api`**.
  This is mandatory and sufficient for the app to enforce access.
- **Frontend UI** (`RequiredRole.svelte`, the user menu) reads the **ID
  token**, whose audience is `estimation-spa`. So the roles the UI shows
  come from app roles defined **and assigned** on **`estimation-spa`**.

Consequences:

- Define the same `VIEWER`/`ESTIMATOR`/`ADMIN` app roles on **both**
  registrations, and assign each test user in **both**, or the UI will show
  no roles even while the API correctly accepts the user's requests.
- (The `dev` auth module doesn't show this split — it builds the account
  with roles directly — which is why it only surfaces under Entra.)
- A cleaner long-term alternative is to have the SPA source roles from the
  backend (`GET /api/auth/me`) instead of the ID token, so roles live only
  on `estimation-api`; that is a frontend change, not done yet.

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

`%dev` and `%test` keep `quarkus.oidc.enabled=false`
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
| `ENTRA_SPA_CLIENT_ID`        | deploy   | SPA app's client id; prechecked by `minikube-deploy.sh` and used to source `VITE_ENTRA_SPA_CLIENT_ID` — not read by the backend at runtime |
| `VITE_AUTH_PROVIDER`         | frontend | `entra` to activate this module                        |
| `VITE_ENTRA_TENANT_ID`       | frontend | mirrors the backend tenant id                          |
| `VITE_ENTRA_SPA_CLIENT_ID`   | frontend | SPA app's client id (MSAL `clientId`)                  |
| `VITE_ENTRA_API_CLIENT_ID`   | frontend | API app's client id (used to derive the access scope)  |
| `VITE_ENTRA_REDIRECT_URI`    | frontend | redirect URI — **optional**, default `http://localhost:5173` |

`ENTRA_SPA_CLIENT_ID` is also enforced by `scripts/minikube-deploy.sh`
as a precheck so deploys cannot silently ship unresolved placeholders.

## Running Entra locally (optional)

To test the Entra wiring against a real tenant from a local checkout
(no minikube), stop `dev.sh`, export the variables above (all except the
optional `VITE_ENTRA_REDIRECT_URI`, which defaults to `http://localhost:5173`),
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

QUARKUS_PROFILE=dev ./gradlew :backend:implementation:quarkusDev
# in another shell:
cd src/frontend && npm run dev
```

`http://localhost:5173` must be a registered redirect URI on
`estimation-spa` for this to work.

## How env vars flow into the minikube backend Pod

`scripts/minikube-deploy.sh` exports `APP_AUTH_PROVIDER=entra`,
`VITE_AUTH_PROVIDER=entra`, and prechecks the three `ENTRA_*` ids before
building the backend and frontend container images with Gradle
(`./gradlew :backend:implementation:imageBuild -Dquarkus.container-image.build=true
:frontend:dockerBuildImage -x test` — backend image via Quarkus/Jib, frontend
via the Gradle Docker task), loading them into minikube via
`docker save <img> | minikube ssh -- docker load` (deliberately **not**
`minikube image load`, which caches the exported tarball and, for a
fixed tag like `1.0.0-SNAPSHOT`, silently reuses the STALE copy so a
rebuilt image never reaches the cluster), then applying the Kustomize
overlay (`kubectl apply -k`) and forcing a `kubectl rollout restart` so
pods pick up the reloaded image and current config. The Pod
inherits these env vars via the existing Deployment manifest's `env:`
forwarding (no manifest edits required); Quarkus resolves
`${ENTRA_TENANT_ID}` etc. in `application.properties` at startup from
process env. If you operate a non-minikube cluster, set the same env
vars on the backend Deployment via your cluster's Secret / ConfigMap
plumbing and the substitution still happens at runtime.

## Troubleshooting

First, **see the reason** rather than guessing. The backend logs the exact
token-rejection cause under the `io.quarkus.oidc` category at DEBUG; the
`dev-minikube` profile enables it (see `docs/development.md` → "Viewing
backend logs on Minikube"). Then reproduce the request and read
`kubectl -n estimation logs -f deploy/backend`. In the browser, DevTools →
Network → the failing `/api/...` request → **Request Headers** tells you
whether an `Authorization: Bearer …` header is even being sent.

Common failures seen during setup:

- **`AADSTS9002326: Cross-origin token redemption is permitted only for the
  'Single-Page Application' client-type`** (during login, in the browser).
  The redirect URI is registered under the **Web** platform on
  `estimation-spa`. Remove it there and add it under **Single-page
  application** (see the `estimation-spa` step above).

- **401 on every `/api/...` call, backend DEBUG shows `Issuer (iss) claim
  value (https://sts.windows.net/<tenant>/) doesn't match expected value of
  https://login.microsoftonline.com/<tenant>/v2.0`.** The API is issuing
  **v1.0** access tokens. Set `accessTokenAcceptedVersion: 2` on
  `estimation-api` (see the `estimation-api` step), then re-acquire a token
  (log out / clear `localStorage`). Audience (`aud`) and scope (`scp:
  access`) being correct while the issuer is wrong is the tell-tale sign.

- **401 and DevTools shows NO `Authorization` header on the request.** The
  SPA isn't logged in or didn't attach a token — check that login completed
  (the user menu shows an account) and the browser console for MSAL errors.
  Not a backend problem.

- **401 and the header IS present but the backend DEBUG says "Bearer access
  token is not available".** The proxy stripped it — confirm nginx forwards
  `Authorization` (it does by default; the frontend `nginx.conf` does not
  clear it).

- **Authenticated but every write returns 403, or the UI shows no roles.**
  The role `value` on the app registration doesn't exactly match
  `VIEWER`/`ESTIMATOR`/`ADMIN`, the user isn't assigned the role, or the
  token predates the assignment (re-acquire it). For the UI specifically,
  see [Roles: enforcement vs. UI display](#roles-enforcement-vs-ui-display)
  — roles must be assigned on `estimation-spa`, not only `estimation-api`.

- **Frontend can't reach the backend at all** (`backend could not be
  resolved`, minikube). Not an Entra issue — an nginx/DNS problem; see
  `docs/development.md`.

## Cross-references

- Modular SPI: `src/backend/implementation/src/main/kotlin/io/github/theestimator/auth/`
  + `src/frontend/src/lib/auth/`.
- Dev module (default for local development): `docs/auth-dev.md`.
- Keycloak module (planned): `task-060`.
