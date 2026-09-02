# Entra ID auth module

The `entra` auth module is the production-grade concrete implementation
of the modular auth SPI declared by task-005. Activation is purely
config-driven and happens at **runtime on both sides** (task-161/task-162): set
`APP_AUTH_PROVIDER=entra` on the backend and `"authProvider": "entra"` in the
SPA's `/config.json`; everything else flows through the SPI. Neither side needs
a rebuild to change stage, and the two values **must agree** — a `dev`
`config.json` against an `entra` backend 401s every call. For local development
the `dev` module (see [authentication.md](./authentication.md)) stays the
default; this document covers production and the optional "developer-Entra"
workflow.

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
       `minikube-deploy.sh` uses for `OIDC_REDIRECT_URI`; if you access the
       SPA via the Ingress host or a different port, register and export
       that URL instead)
     - `https://estimation.<your-domain>` (production)
   - **API permissions** → Add → My APIs → `estimation-api` →
     Delegated `access`. Grant admin consent.
   - **App roles for the SPA UI — no longer required (task-120).** The
     frontend now reads roles from the backend (`GET /api/auth/me`), not from
     the SPA's ID token, so roles only need to be defined and assigned on
     `estimation-api` (below). You do **not** need to define or assign app
     roles on `estimation-spa` for the UI to show them. (Defining them here is
     harmless but redundant.) See
     [Roles: enforcement vs. UI display](#roles-enforcement-vs-ui-display).

3. Assign at least one **test user per role** to `estimation-api` via
   the tenant's *Enterprise Applications → estimation-api → Users
   and groups* page. task-008's manual verification checklist
   requires one VIEWER, one ESTIMATOR, and one ADMIN test user.

4. Record three identifiers in a secure place (NEVER commit):
   - `ENTRA_TENANT_ID` — the tenant GUID.
   - `ENTRA_API_CLIENT_ID` — `estimation-api`'s Application (client) ID.
   - `ENTRA_SPA_CLIENT_ID` — `estimation-spa`'s Application (client) ID.

## Roles: enforcement vs. UI display

**Single source of truth — `estimation-api` (task-120).** Both backend
enforcement and the frontend UI now derive roles from the **access token**
(audience `estimation-api`):

- **Backend enforcement** (`@RolesAllowed`, the 401/403 decision) reads the
  access token directly.
- **Frontend UI** (`RequiredRole.svelte`, the user menu) reads roles from the
  backend via `GET /api/auth/me` (the Entra provider's `loadAccount()` in
  `EntraAuthProvider.ts` → `$lib/auth/currentUser.ts`), which the backend
  computes from that same access token.

Consequences:

- Define and assign the `VIEWER`/`ESTIMATOR`/`ADMIN` app roles **only on
  `estimation-api`**. The UI and the backend can never disagree because they
  read the same roles.
- The `estimation-spa` app-role assignment is **no longer required** for the
  UI (it was, before task-120, when the UI read the ID token).
- (The `dev` auth module builds its account with roles client-side, which
  already agrees with the backend dev augmentor — so this was never an issue
  under dev.)

## Backend wiring

The `entra` module is implemented in
`src/backend/implementation/src/main/kotlin/io/pythia/auth/entra/`:

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

### Where the OIDC coordinates come from

`%dev-minikube` in
`src/backend/implementation/src/main/resources/application.properties` still
carries a full local set, expanded from the `ENTRA_*` variables at startup:

```
%dev-minikube.quarkus.oidc.auth-server-url=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0
%dev-minikube.quarkus.oidc.client-id=${ENTRA_API_CLIENT_ID}
%dev-minikube.quarkus.oidc.application-type=service
%dev-minikube.quarkus.oidc.token.audience=api://${ENTRA_API_CLIENT_ID},${ENTRA_API_CLIENT_ID}
%dev-minikube.quarkus.oidc.roles.role-claim-path=roles
```

**`%prod` does not** (task-161). A stage differs from another stage by its
configuration, never by its image, so every stage-varying value left the
properties file and arrives as an environment variable from the `backend-config`
ConfigMap instead — `APP_AUTH_PROVIDER`, `QUARKUS_OIDC_AUTH_SERVER_URL`,
`QUARKUS_OIDC_CLIENT_ID`, `QUARKUS_OIDC_TOKEN_AUDIENCE` and the datasource
coordinates. `%prod` keeps only the stage-invariant remainder
(`application-type`, `roles.role-claim-path`, `db-kind`, the Flyway and schema
flags, JSON logging), and **no property there may contain a `${...}`
placeholder** — expanding one from the pod environment would be a second
configuration mechanism, which is how production once came to reference an
`ENTRA_API_CLIENT_ID` that no production manifest supplied. Note also that
`quarkus.oidc.enabled` is build-time (`OidcBuildTimeConfig`) and therefore inert
as an env var; the runtime lever is `quarkus.oidc.tenant-enabled`, and whether
OIDC is *used* is governed by `APP_AUTH_PROVIDER`. See
[deployment.md](./deployment.md) for the full key table.

The audience is listed **twice on purpose**: v1.0 access tokens carry
`aud=api://<api-client-id>`, but v2.0 tokens (`accessTokenAcceptedVersion: 2`)
carry the bare `aud=<api-client-id>`. Listing both makes the backend accept a
token regardless of its version — the token is accepted if `aud` matches any
configured value.

`%dev` and `%test` keep `quarkus.oidc.enabled=false`
because the dev module covers those paths.

## Frontend wiring

The MSAL-based provider lives in
`src/frontend/src/lib/auth/EntraAuthProvider.ts` and uses
`@azure/msal-browser` (NOT msal-react — this project is Svelte 5).
Flow:

Its coordinates come from the **runtime** config (task-162), not from
`VITE_*` inlined at build time: `$lib/config/runtimeConfig.ts` fetches
`/config.json` once from the root `+layout.ts` load and validates it, so
`getRuntimeConfig().entra` is available before any component initialises. All
four fields — `tenantId`, `spaClientId`, `apiClientId`, `redirectUri` — are
**required**; the loader fails closed with a translated error rather than
falling back to `dev` or to `http://localhost:5173`.

- `init()` → instantiate `PublicClientApplication` with
  `clientId=entra.spaClientId`,
  `authority=https://login.microsoftonline.com/<entra.tenantId>`, the redirect
  URI `entra.redirectUri`, and `cache.cacheLocation: 'localStorage'`. Calls
  `msal.initialize()` and `handleRedirectPromise()` so the post-login
  redirect is processed.
- `login()` → `msal.loginRedirect({ scopes: ['api://<entra.apiClientId>/access'] })`.
  Redirect (not popup) for robustness against popup blockers.
- `logout()` → `msal.logoutRedirect()`.
- `loadAccount()` → returns `null` when MSAL holds no account (so `RequireAuth`
  triggers the login redirect); otherwise fetches `GET /api/auth/me` through
  `fetchCurrentUserAccount()` and caches the result. **Roles come from the
  backend, not from the id token** (task-120) — which is why app roles only need
  to be defined and assigned on `estimation-api`, and why the UI can never
  disagree with backend enforcement.
- `getAccount()` → returns that cached `AuthAccount` synchronously.
- `getAuthorizationHeader()` → `msal.acquireTokenSilent` for the API
  scope; on `InteractionRequiredAuthError` falls back to
  `acquireTokenRedirect`. Returns `"Bearer <accessToken>"`.

## Where each coordinate lives

**What a developer supplies.** `scripts/minikube-deploy.sh` prechecks the three
`ENTRA_*` ids and refuses to deploy without them, so a deploy can never ship an
unresolved placeholder. Keep them outside the repository (see step 4 of the
tenant setup); none of them is a secret, but none belongs in a public history
either.

| variable              | description                                          |
|-----------------------|------------------------------------------------------|
| `ENTRA_TENANT_ID`     | the tenant GUID                                       |
| `ENTRA_API_CLIENT_ID` | `estimation-api`'s client id — drives `client-id` + audience |
| `ENTRA_SPA_CLIENT_ID` | `estimation-spa`'s client id (MSAL `clientId`); not read by the backend |
| `OIDC_REDIRECT_URI`   | **optional**; defaults to the port-forward URL `http://localhost:8080` |

**What the manifests consume.** Every overlay uses one provider-neutral
`${OIDC_*}` convention carrying **full values**, never identity-provider-specific
fragments a manifest then composes into a URL — the auth layer is modular
(`dev` | `entra` | `keycloak`), so the manifests are not named after one
provider. `minikube-deploy.sh` derives these from the `ENTRA_*` ids above; the
internal GitLab pipeline supplies them as masked CI/CD variables.

| name | goes into | becomes |
|---|---|---|
| `APP_AUTH_PROVIDER` | `backend-config` ConfigMap | `app.auth.provider` |
| `OIDC_AUTH_SERVER_URL` | `backend-config` | `QUARKUS_OIDC_AUTH_SERVER_URL` — `https://login.microsoftonline.com/<tenant>/v2.0` |
| `OIDC_CLIENT_ID` | `backend-config` **and** `frontend-config` | `QUARKUS_OIDC_CLIENT_ID`; reused as the SPA's `apiClientId` |
| `OIDC_TOKEN_AUDIENCE` | `backend-config` | `QUARKUS_OIDC_TOKEN_AUDIENCE` — both audience forms, comma-separated |
| `OIDC_TENANT_ID` | `frontend-config` | `config.json` → `entra.tenantId` |
| `OIDC_SPA_CLIENT_ID` | `frontend-config` | `config.json` → `entra.spaClientId` |
| `OIDC_REDIRECT_URI` | `frontend-config` | `config.json` → `entra.redirectUri` |

There are **no `VITE_*` variables any more** (task-162). Vite inlined them at
build time, which made the frontend image *be* the configuration: a stage needed
its own build and an image could not be promoted. `src/frontend/src/vite-env.d.ts`
still declares the old names, but nothing reads them.

## Running Entra locally (optional)

To test the Entra wiring against a real tenant from a local checkout (no
minikube), stop `dev.sh` and start Quarkus + Vite manually.
**`./scripts/minikube-deploy.sh` is the supported path; this local variant is
covered by no test and needs both sides overridden by hand.**

**Backend.** `%dev` runs the dev module and sets `quarkus.oidc.enabled=false`,
and it carries no issuer of its own — so switching the provider alone is not
enough. `quarkusDev` augments in-process, so the build-time OIDC keys can be
supplied as environment variables at launch:

```bash
export APP_AUTH_PROVIDER=entra
export QUARKUS_OIDC_ENABLED=true
export QUARKUS_OIDC_AUTH_SERVER_URL="https://login.microsoftonline.com/<tenant-guid>/v2.0"
export QUARKUS_OIDC_CLIENT_ID=<api-client-id>
export QUARKUS_OIDC_TOKEN_AUDIENCE="api://<api-client-id>,<api-client-id>"

QUARKUS_PROFILE=dev ./gradlew :backend:implementation:quarkusDev
```

Do **not** `export` these into the shell you then run `./gradlew build` in:
`APP_AUTH_PROVIDER=entra` augments the OIDC-less test profile with the Entra
provider and the build fails with a cryptic ArC `UnsatisfiedResolutionException`.
(This is the same trap `minikube-deploy.sh` refuses to be `source`d over.)

**Frontend.** Overwrite the `predev`-generated `src/frontend/static/config.json`
— it is git-ignored, and `static/config.example.json` documents the shape.
`npm run dev` regenerates it as `{"authProvider":"dev"}` on every start, so write
it *after* starting Vite, or start Vite with `npx vite dev`:

```json
{
  "authProvider": "entra",
  "entra": {
    "tenantId": "<tenant-guid>",
    "spaClientId": "<spa-client-id>",
    "apiClientId": "<api-client-id>",
    "redirectUri": "http://localhost:5173"
  }
}
```

`http://localhost:5173` must be a registered redirect URI on `estimation-spa`
under the **Single-page application** platform for this to work. All four
`entra` fields are required — the loader rejects a missing one rather than
guessing a default.

## How the configuration reaches the minikube Pods

`scripts/minikube-deploy.sh` prechecks the three `ENTRA_*` ids, derives the
`OIDC_*` names from them, exports `APP_AUTH_PROVIDER=entra` for the backend
image build, and then:

1. builds both container images with Gradle
   (`./gradlew :backend:implementation:imageBuild -Dquarkus.container-image.build=true
   :frontend:dockerBuildImage -x test` — backend via Quarkus/Jib, frontend via
   the Gradle Docker task);
2. loads them into minikube with `docker save <img> | minikube ssh -- docker load`
   — deliberately **not** `minikube image load`, which caches the exported
   tarball and, for a fixed tag like `1.0.0-SNAPSHOT`, silently reuses the STALE
   copy so a rebuilt image never reaches the cluster;
3. renders `k8s/overlays/minikube` and resolves the `${OIDC_*}` placeholders
   with `envsubst` before `kubectl apply` — kustomize cannot read the
   environment, and these are per-user ids that are not committed;
4. forces a `kubectl rollout restart` so pods pick up the reloaded image and the
   current ConfigMaps.

**Nothing is baked into the image and no `ENTRA_*` variable reaches a Pod.** The
backend Deployment pulls its whole environment from the `backend-config`
ConfigMap via `envFrom` (plus `QUARKUS_DATASOURCE_USERNAME`/`PASSWORD` from the
`postgres-credentials` Secret), and the frontend Deployment mounts
`frontend-config`'s `config.json` at `/usr/share/nginx/html/config.json` with
`subPath`. `subPath` means the file does not update without a pod restart —
which is what we want, since the SPA reads it once at boot.

On a non-minikube cluster, supply the same ConfigMap/Secret keys; the
`overlays/staging` and `overlays/production` overlays show the shape, and
`scripts/deploy.sh` discovers every `${...}` placeholder in the rendered
manifests and refuses to deploy while one is unset.

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

- **401, backend DEBUG shows `Audience (aud) claim [<api-client-id>] doesn't
  contain an acceptable identifier. Expected api://<api-client-id> as an aud
  value`.** The flip side of setting `accessTokenAcceptedVersion: 2`: v2.0
  access tokens carry the **bare** `aud=<api-client-id>` (a GUID), whereas
  v1.0 tokens carried `aud=api://<api-client-id>`. The audience must list
  **both** forms (`api://${ENTRA_API_CLIENT_ID},${ENTRA_API_CLIENT_ID}`). On
  **minikube this lives in the k8s overlay env var**
  `QUARKUS_OIDC_TOKEN_AUDIENCE` (`k8s/overlays/minikube/backend-oidc.yaml`),
  which **overrides** the image's baked `application.properties` — so editing
  only `application.properties` has no effect on the cluster; fix the overlay
  and re-apply (`./scripts/minikube-deploy.sh`, or re-apply the ConfigMap +
  `kubectl -n estimation rollout restart deployment/backend`). No image rebuild
  is needed for an env-var change. The token reaching the OIDC layer
  (issuer/scope correct) while only the audience is rejected is the tell-tale
  sign — and note this is a *validation* failure (the token IS read), distinct
  from `Bearer access token is not available` (no token at all).

- **401 and DevTools shows NO `Authorization` header on the request.** The
  SPA isn't logged in or didn't attach a token — check that login completed
  (the user menu shows an account) and the browser console for MSAL errors.
  Not a backend problem.

- **401 and the header IS present but the backend DEBUG says "Bearer access
  token is not available".** The proxy stripped it — confirm nginx forwards
  `Authorization` (it does by default; the frontend `nginx.conf` does not
  clear it).

- **Authenticated but every write returns 403, or the UI shows no roles.**
  The role `value` on the `estimation-api` app registration doesn't exactly
  match `VIEWER`/`ESTIMATOR`/`ADMIN`, the user isn't assigned the role on
  `estimation-api`, or the token predates the assignment (re-acquire it).
  Since task-120 the UI reads roles from `GET /api/auth/me` (same access
  token as the backend), so assigning on `estimation-api` covers both — see
  [Roles: enforcement vs. UI display](#roles-enforcement-vs-ui-display).

- **Frontend can't reach the backend at all** (`backend could not be
  resolved`, minikube). Not an Entra issue — an nginx/DNS problem; see
  `docs/development.md`.

## Cross-references

- Modular SPI: `src/backend/implementation/src/main/kotlin/io/pythia/auth/`
  + `src/frontend/src/lib/auth/`.
- Dev module (default for local development): [`docs/authentication.md`](./authentication.md).
- Keycloak module (planned): `task-060`.
