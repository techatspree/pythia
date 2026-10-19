# Deployment

How Pythia reaches its corporate environment, and what an operator needs to know
when something breaks at 2am.

> **This document deliberately contains no hostnames, registry addresses, SSH
> targets or credentials.** They live as masked GitLab CI/CD variables. The
> repository is public under Apache-2.0 and the project's history was rewritten
> once already to remove internal identifiers — do not put them back.

## The environment

- **One server**, inside the corporate private network, reached over **SSH**.
- It runs **minikube** — a single-node Kubernetes cluster, one profile. Kustomize
  is therefore the deployment unit and `k8s/overlays/` applies unchanged; SSH is
  how the pipeline reaches the cluster, not an alternative to it.
- **Staging and production are two namespaces on that one cluster:**
  `estimation-staging` and `estimation`.
- The cluster pulls the **released images**: straight from GHCR, or from an
  internal registry the downstream pipeline copied them into.

```
GitHub (public)                       internal GitLab                  the server
─────────────────                     ───────────────                  ──────────
tag vX.Y.Z on main
  └─ CI publish job
       ├─ images ──▶ GHCR  ◀── (optional copy) ── pipeline polls releases
       └─ release + deploy bundle ──────────────▶  ├─ download + verify bundle
                                                   ├─ deploy staging ──ssh──▶ minikube
                                                   │                          ns estimation-staging
                                                   ├─ ⏸ manual approval
                                                   └─ deploy production ─ssh─▶ ns estimation
```

### Why the pipeline is not in this repository

CI — build, test, Playwright, image build — runs on **GitHub Actions**
(`.github/workflows/ci.yaml`), because that is where pull requests land and it
must be visible to outside contributors. It holds no secrets. On a release tag
it also **publishes** the release (task-199): both images to GHCR and a deploy
bundle to the GitHub Release. It never deploys anything.

Deployment cannot run there: a GitHub-hosted runner cannot reach a private
corporate network. It runs on an internal GitLab runner as a **consumer of
releases** (`<internal-group>/<deploy-project>`): it downloads a release's
bundle and runs `deploy.sh` from it. It needs no copy of this repository and
builds nothing, and its definition stays internal so that internal coordinates
never enter a public git history. What stays here is everything generic and
reviewable: the manifests, `scripts/deploy.sh`, `scripts/smoke.sh`, and the
release contract below.

## What a release publishes

Every release `vX.Y.Z` (see "Releasing" below) publishes, from the CI `publish`
job, everything a deployment needs. The layout and the `release.env` keys are a
**public contract**: downstream consumers depend on them, not on this
repository's layout, so they change only deliberately.

**Both images, multi-arch, on GHCR**, public:

```
ghcr.io/<owner>/pythia/pythia-backend:X.Y.Z
ghcr.io/<owner>/pythia/pythia-frontend:X.Y.Z
```

`<owner>` is this repository's GitHub owner, lower-cased. The image tag is the
application version WITHOUT the `v` — the same string as `gradle.properties`
and the API's `info.version`. The `pythia/pythia-*` part equals the names the
overlays use (`${REGISTRY}/pythia/pythia-backend`), so `REGISTRY=ghcr.io/<owner>`
pulls straight from GHCR, and a registry holding copies keeps the same paths.
Each image carries the `org.opencontainers.image.source` label pointing at this
repository.

**A deploy bundle**, attached to the GitHub Release of the tag:
`pythia-deploy-vX.Y.Z.tar.gz` plus `pythia-deploy-vX.Y.Z.tar.gz.sha256`.

```
pythia-deploy-v1.2.0/
  release.env                 the release's identity, see below
  LICENSE NOTICE CONTRIBUTORS Apache-2.0 requires NOTICE to travel
                              along; CONTRIBUTORS defines its holder
  scripts/deploy.sh
  scripts/smoke.sh
  k8s/base/**
  k8s/overlays/staging/**
  k8s/overlays/production/**  (NOT the minikube overlay)
```

`release.env` holds exactly these six `KEY=value` lines — no quoting, no
comments:

```
PYTHIA_VERSION=v1.2.0
SOURCE_COMMIT=<40-hex commit the tag points at>
BACKEND_IMAGE=ghcr.io/<owner>/pythia/pythia-backend
BACKEND_DIGEST=sha256:<64 hex>
FRONTEND_IMAGE=ghcr.io/<owner>/pythia/pythia-frontend
FRONTEND_DIGEST=sha256:<64 hex>
```

**The digest, not the tag, is the contract.** Each digest is that of the
multi-arch INDEX the release run pushed; the publish job checks it equals what
GHCR serves and what Jib recorded for its own push. A tag can be overwritten, a
digest cannot — and our image build is not byte-reproducible (see below), so a
re-run would produce different digests under the same tag.

The bundle is built by `scripts/release-bundle.sh` with `git archive` from the
tag, so it holds exactly the tagged content and is byte-reproducible: the same
inputs give the same SHA-256.

### The SBOM

Every release also attaches **one CycloneDX 1.6 SBOM for the whole
application** (task-206), `pythia-vX.Y.Z.cdx.json`, with its checksum file
`pythia-vX.Y.Z.cdx.json.sha256`. It is its own release asset; the deploy bundle
does not contain it. Its structure:

```
pythia (X.Y.Z, Apache-2.0)                     metadata.component
├─ pythia-backend   the jars Quarkus packages  components[0].components
└─ pythia-frontend  the npm packages bundled   components[1].components
                    into the SPA, plus the
                    Kotlin/JS code compiled
                    into domain.mjs
```

Each library carries its purl and license. A library `bom-ref` is prefixed
with its part (`pythia-backend:…`), because the same library can ship in both
parts, our own `io.pythia` modules for instance. The `dependencies` section
holds the graph, from `pythia` down.

It describes the **application**, not the base images' operating-system
packages (Red Hat UBI 9, Alpine). Those are covered by the CI Trivy scan for
vulnerabilities and were reviewed once for licenses (task-208). Verify it like
the bundle: `sha256sum -c pythia-vX.Y.Z.cdx.json.sha256`.

### Verifying and copying a release

```bash
V=v1.2.0
sha256sum -c pythia-deploy-$V.tar.gz.sha256
tar -xzf pythia-deploy-$V.tar.gz
cat pythia-deploy-$V/release.env
```

To serve the images from an internal registry instead of GHCR, copy them **by
digest**, keeping every platform and the digest itself:

```bash
E=pythia-deploy-$V/release.env
BACKEND_DIGEST=$(sed -n 's/^BACKEND_DIGEST=//p' "$E")
FRONTEND_DIGEST=$(sed -n 's/^FRONTEND_DIGEST=//p' "$E")
skopeo copy --all --preserve-digests \
  docker://ghcr.io/<owner>/pythia/pythia-backend@$BACKEND_DIGEST \
  docker://$REGISTRY/pythia/pythia-backend:1.2.0
skopeo copy --all --preserve-digests \
  docker://ghcr.io/<owner>/pythia/pythia-frontend@$FRONTEND_DIGEST \
  docker://$REGISTRY/pythia/pythia-frontend:1.2.0
```

`--all` keeps both platforms of the index; `--preserve-digests` keeps the
digest the overlays pin, so `deploy.sh` resolves the same image from the copy.
The values are extracted, never `source`d: the bundle is downloaded data, and
sourcing it would execute whatever a tampered `release.env` contains. `deploy.sh`
parses it the same way.

### How the images are built

**Pushed images are multi-arch** (task-196): any push builds `linux/amd64` +
`linux/arm64` as one index, for both images, without asking —
`-Dquarkus.container-image.push=true` (or the plugin's `imagePush` task) for the
backend, `-PfrontendImagePush=true` for the frontend. Each node pulls its native
variant. Only a build into a local Docker daemon is single-platform
(`linux/amd64`), because a daemon load takes one image, not an index.

**No image build needs a Docker daemon** (task-195). The backend pushes through
Quarkus/Jib with `-Dquarkus.container-image.push=true`, and the frontend through
jib-core. Registry credentials for the frontend come from
`~/.docker/config.json` and the well-known credential helpers, or explicitly
from `ORG_GRADLE_PROJECT_frontendImageUsername` /
`ORG_GRADLE_PROJECT_frontendImagePassword`. Use those environment variables,
not `-P`, so the password stays off the command line.

**The image build takes no auth provider** (task-194). A production build always
contains the Entra module and never the dev one, so the image stays
stage-independent; the publish job re-checks this on the very augmentation it
pushes (`scripts/verify-auth-modules.sh`). The ConfigMap's `APP_AUTH_PROVIDER`
must name a module in the build: otherwise `AuthProviderGuard` refuses to start,
logging the provider and the modules the build does contain, rather than
starting a backend that authenticates nobody.

Do **not** change the reproducibility settings: both
`quarkus.jib.use-current-timestamp*` stay `false`, both base images stay pinned
by digest, and no time- or environment-derived jar-manifest attribute may be
added. The GitHub `images` job asserts exactly those inputs and goes red if one
is broken.

It deliberately does **not** assert that the image is byte-identical across
rebuilds, because it is not: Quarkus' augmentation emits one non-deterministic
file (`quarkus/generated-bytecode.jar`, in it the recorded
`ResteasyReactiveProcessor$setupDeployment*.class`). Keeping the inputs constant
is what makes a build independent of *when* and *where* it ran; it does not make
two builds bit-for-bit equal. That is exactly why a release is pinned by digest.

## The downstream pipeline

`<internal-group>/<deploy-project>` deploys **releases only**. Its stages:

1. **Find the release.** Poll for the newest `v*` tag (or take one as a
   pipeline input), download its bundle and `.sha256` from the GitHub Release,
   and verify the checksum.
2. **Optionally copy the images** into the internal registry by digest (see
   "Verifying and copying a release").
3. **Deploy staging** from the unpacked bundle.
4. **Manual approval** — a GitLab job with `when: manual`, on a protected
   environment so only authorised users can trigger it.
5. **Deploy production** from the same bundle.
6. **Smoke test** after each rollout.

Each deploy step copies the unpacked bundle to the server and runs `deploy.sh`
*there*, where the kubeconfig and the cluster are local. `release.env` supplies
both digests, so the pipeline sets only the stage's coordinates:

```bash
scp -r "pythia-deploy-$V" "$DEPLOY_HOST:/opt/pythia/releases/"
ssh "$DEPLOY_HOST" "cd /opt/pythia/releases/pythia-deploy-$V \
  && REGISTRY='$REGISTRY' INGRESS_HOST='$INGRESS_HOST' \
     OIDC_AUTH_SERVER_URL='$OIDC_AUTH_SERVER_URL' OIDC_CLIENT_ID='$OIDC_CLIENT_ID' \
     OIDC_TOKEN_AUDIENCE='$OIDC_TOKEN_AUDIENCE' ... \
     ./scripts/deploy.sh production"
ssh "$DEPLOY_HOST" "cd /opt/pythia/releases/pythia-deploy-$V && ./scripts/smoke.sh https://$INGRESS_HOST"
```

The manifests and the images come from the same release by construction: the
bundle carries both the manifests of that tag and the digests of its images.

**Do not expose the minikube API server to the runner.** Its endpoint is the
driver's internal IP; publishing it means certificate SANs and a firewall hole,
and buys nothing over SSH.

### CI/CD variables

All masked; all protected on the production job. Descriptions only — never
record a value in this file.

| Variable | Holds |
|---|---|
| `REGISTRY` | Where the cluster pulls the images from: `ghcr.io/<owner>`, or the internal registry (host and optional path prefix) holding copies. |
| `BACKEND_DIGEST` / `FRONTEND_DIGEST` | Not a CI variable: they come from the bundle's `release.env`. Set them by hand only when deploying from a git checkout (`deploy.sh` refuses a value that contradicts a bundle). |
| `INGRESS_HOST` | Public hostname for the environment being deployed. |
| `OIDC_AUTH_SERVER_URL` | Issuer URL of the identity provider realm. |
| `OIDC_CLIENT_ID` | Client id registered for that environment. |
| `OIDC_TOKEN_AUDIENCE` | Accepted token audience(s), comma-separated. See "Configuration is not in the image" below. |
| `OIDC_TENANT_ID` | Identity-provider tenant id, for the SPA's `config.json` (task-162). |
| `OIDC_SPA_CLIENT_ID` | Client id of the **SPA** registration — distinct from `OIDC_CLIENT_ID`, which is the API's. |
| `OIDC_REDIRECT_URI` | Where the identity provider returns the browser. Must be a **registered** redirect URI on the SPA registration and match how the SPA is opened. |
| `POSTGRES_PASSWORD` | Database password for the stage. See "Rotating the database password" below. |
| `DEPLOY_HOST` | SSH target of the server (`user@host`). |
| `DEPLOY_SSH_KEY` | Private key for that SSH user. File-type variable. |
| `REGISTRY_USER` / `REGISTRY_PASSWORD` | Pull credentials for the cluster's image pull secret (see "Registry pull secret"); and push credentials when the pipeline copies images. Not needed when the cluster pulls the public GHCR images. |
| `SMOKE_TOKEN` | Optional bearer token so the smoke test can assert an authenticated 200 rather than a 401. |

### Runner requirements

No Docker and no build toolchain: the runner builds nothing. It needs HTTPS to
`github.com` (tags and release assets, which redirect to a
`*.githubusercontent.com` host), to `ghcr.io` (image blobs redirect to
`pkg-containers.githubusercontent.com`) if it copies images or the cluster pulls
from GHCR, and to the internal registry if it copies into one. `skopeo` is
needed only for copying. The server itself needs to reach whichever registry
`REGISTRY` names.

## Configuration is not in the image

**A stage differs from another stage by its configuration, never by its image**
(task-161). One backend image serves minikube, staging and production; what
differs is the `backend-config` ConfigMap and the `postgres-credentials` Secret.

Quarkus maps each key back to its property through MicroProfile Config
(`QUARKUS_OIDC_CLIENT_ID` → `quarkus.oidc.client-id`), so nothing in the
application reads these names directly.

| Key | Source | Holds |
|---|---|---|
| `APP_AUTH_PROVIDER` | ConfigMap | `entra` for a deployed stage. Was baked as `%prod.app.auth.provider`. It must name an auth module in the build — a production build contains only Entra — or `AuthProviderGuard` refuses to start (task-194). |
| `QUARKUS_DATASOURCE_JDBC_URL` | ConfigMap | JDBC URL of the stage's database. |
| `QUARKUS_DATASOURCE_USERNAME` | Secret `postgres-credentials` | Database user. |
| `QUARKUS_DATASOURCE_PASSWORD` | Secret `postgres-credentials` | Database password. Placeholder `${POSTGRES_PASSWORD}` in the staging and production overlays. A default one used to be committed in `application.properties`, and then in the base Secret; the base still carries the local-minikube value. |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | ConfigMap | Issuer URL. Placeholder `${OIDC_AUTH_SERVER_URL}`. |
| `QUARKUS_OIDC_CLIENT_ID` | ConfigMap | Client id. Placeholder `${OIDC_CLIENT_ID}`. |
| `QUARKUS_OIDC_TOKEN_AUDIENCE` | ConfigMap | Accepted audiences. Placeholder `${OIDC_TOKEN_AUDIENCE}`. |
| `config.json` | ConfigMap `frontend-config` | The SPA's runtime config (task-162). Placeholders `${OIDC_TENANT_ID}`, `${OIDC_SPA_CLIENT_ID}`, `${OIDC_REDIRECT_URI}`, and `${OIDC_CLIENT_ID}` reused as `apiClientId`. |

### Rotating the database password

The base `postgres-credentials` Secret carries `estimation` — the local-minikube
value, matching `%dev-minikube.quarkus.datasource.password`. The staging and
production overlays replace it with `${POSTGRES_PASSWORD}`, which `deploy.sh`
refuses to leave unset, so a deployed stage cannot fall back to it.

**`POSTGRES_PASSWORD` only takes effect when the data directory is
initialised.** The `postgres` image runs `initdb` on an empty PVC and ignores
the variable on every later start. So applying a new password against a
database that already exists changes only what the *backend* presents, and the
next backend restart fails to authenticate — with a healthy-looking PostgreSQL
next to it.

On a database that already has data, change it in PostgreSQL first, then deploy:

```bash
kubectl -n estimation exec -it statefulset/postgres -- \
    psql -U estimation -c "ALTER USER estimation PASSWORD 'the-new-one';"
# then set POSTGRES_PASSWORD in CI/CD and redeploy, or for a config-only change:
kubectl -n estimation rollout restart deployment/backend
```

The rollout restart is needed because a Secret change alone does not restart a
pod: the Deployment spec is unchanged, so `rollout status` reports success
immediately while the pods keep the old value.

On a **fresh** cluster none of this applies — set the variable before the first
deploy and `initdb` picks it up.

### The frontend needs a FILE, not environment variables

Vite inlines `VITE_*` at **build** time, so the frontend image used to *be* the
configuration: a stage needed its own build and an image could not be promoted.
A compiled bundle cannot read a pod environment variable either — only the
browser can read what the server sends it. So the SPA's coordinates are
projected as a **file**: `frontend-config` is mounted at
`/usr/share/nginx/html/config.json` with `subPath`, and the SPA fetches
`/config.json` once at boot.

`subPath` means the file does **not** update without a pod restart. That is the
behaviour we want: the SPA reads it once, so a live-changing file would leave
open tabs disagreeing with newly-opened ones. A config change needs a rollout,
exactly like an image change.

**`config.json`'s `authProvider` must match `APP_AUTH_PROVIDER`.** If they
disagree the SPA sends a `Dev` header to an Entra backend and every call 401s.
The base ConfigMap carries the literal `"UNCONFIGURED"`, which the SPA rejects,
so an overlay that forgets to override it fails loudly rather than quietly
serving dev auth.

`scripts/minikube-deploy.sh` resolves these with a **hardcoded envsubst
allow-list** — a placeholder missing from it is emitted literally rather than
substituted. `scripts/deploy.sh` instead discovers placeholders and refuses to
deploy when one is unset, so it needs no maintenance.

### One placeholder convention

Every overlay uses provider-neutral **`${OIDC_*}`** names carrying **full
values** — never identity-provider-specific fragments a manifest then composes
into a URL. The auth layer is modular (`dev` | `entra` | `keycloak`), so the
manifests are not named after one provider. `scripts/minikube-deploy.sh` still
asks the developer for the `ENTRA_*` ids and derives these names from them.

`scripts/deploy.sh` **discovers** the `${...}` placeholders in the rendered
manifests and refuses to deploy when one is unset, so adding a placeholder needs
no change to that script — but it must be added to the table above.

### What deliberately stays in the image

`%prod` in `application.properties` keeps only **stage-invariant** values:
`quarkus.datasource.db-kind`, `hibernate-orm.schema-management.strategy`,
`flyway.migrate-at-start`, `oidc.application-type`, `oidc.roles.role-claim-path`
and the JSON logging flag. No property there may contain a `${...}` placeholder:
expanding one from the pod environment is a *second* configuration mechanism,
and having both is how production came to reference an `ENTRA_API_CLIENT_ID`
that no production manifest supplied.

Some Quarkus properties are also **fixed at augmentation (build) time** and
cannot be overridden by an environment variable at all — a container that starts
is not proof a value took effect. `quarkus.datasource.db-kind` is one.

> **`QUARKUS_OIDC_ENABLED` is one of these and is inert at runtime.**
> `quarkus.oidc.enabled` lives in `OidcBuildTimeConfig`, so the key present in
> `backend-config` cannot switch OIDC on or off in a built image. It is kept
> because removing it is a behaviour change; the runtime lever is
> `quarkus.oidc.tenant-enabled` (`QUARKUS_OIDC_TENANT_ENABLED`). Whether OIDC is
> actually *used* is governed by `APP_AUTH_PROVIDER`.

The backend logs what it resolved at startup — `AuthProviderGuard` writes the
active auth provider at INFO and the issuer, client id and audience at DEBUG —
so a mis-set key shows up in the pod log instead of as an unexplained 401.

## Releasing

Pythia's releases are defined here, publicly, because a release is a property of
the project, not of any one installation (task-193). A downstream deployment such
as `<internal-group>/<deploy-project>` deploys only releases.

**A release is an annotated git tag `v<MAJOR>.<MINOR>.<PATCH>`** (e.g. `v1.2.0`; no
pre-release or build suffix) on a commit that is on `main`. CI attaches a GitHub
Release to it carrying the deploy bundle (see "What a release publishes"); the
tag is what identifies the release.

**The application version is the release version.** It lives in ONE line,
`version=` in the root `gradle.properties`:

- between releases it is a `-SNAPSHOT` (e.g. `1.0.1-SNAPSHOT`);
- a release is a commit that sets it to `X.Y.Z`, and the tag `vX.Y.Z` sits on
  exactly that commit;
- the next commit moves `main` to the following SNAPSHOT.

So a release build identifies itself everywhere: the backend and frontend image
tags, the jar manifest (`Implementation-Version`), and the OpenAPI document's
`info.version`. The committed OpenAPI client (`src/frontend/src/lib/api/openapi.json`)
is therefore regenerated in every release commit, deliberately. The drift gate
then guarantees the client in a release matches that exact backend.

### Cutting a release

```bash
./scripts/release.sh 1.2.0            # main continues as 1.2.1-SNAPSHOT
./scripts/release.sh 1.3.0 --next 1.4.0-SNAPSHOT
./scripts/release.sh 1.2.0 --dry-run  # every check, no change
```

The script refuses to start unless the working tree is clean, you are on `main`
and it equals `origin/main`, `gradle.properties` holds a `-SNAPSHOT` whose base
is not above the release, and the tag is new and greater than every existing
`v*` tag. It then builds the release (tests included unless `--skip-tests`),
commits "Release X.Y.Z", tags it, commits "Start <next>", shows a summary, asks
for confirmation, and pushes with `git push --atomic origin main refs/tags/vX.Y.Z`.
`main` and the tag land together or not at all. If the push is rejected
(someone pushed to `main` in between), nothing was published. The script prints
how to discard the local release (`git reset --hard origin/main`,
`git tag -d vX.Y.Z`), and you rerun it.

CI builds the tag with the same gate as `main`, and its `release-tag` job
re-checks that the tag is strict semver, on `main`, and on a commit whose
`version=` equals the tag. Only when every gating job has passed does the
`publish` job push the images and create the release.

**The first release is `v1.0.0`:** `./scripts/release.sh 1.0.0`.

### What `publish` does, and on a re-run

It runs only for `v*` tags, after `build`, `e2e`, `images` and `release-tag`.
It refuses outright if a **published** release for the tag already exists —
releases are immutable; tag the next patch version. A **draft** left by a failed
run is deleted and recreated (the tag stays). It pushes the backend, checks that
augmentation for the Entra-not-dev auth module, pushes the frontend, reads both
index digests back from GHCR (both platforms present, and equal to what Jib
recorded), builds the bundle, creates the release as a draft with both assets,
and only then publishes it. A consumer can therefore only see a release whose
images exist and passed the checks; a failed run leaves at most unreferenced
images and a draft.

The job uses the built-in `GITHUB_TOKEN` with `contents: write` and
`packages: write` granted to that job alone, and no secret. It derives the
owner from the repository it runs in, so it runs unchanged in a fork — which is
how it is tested before a real release: cut a throwaway tag in a personal fork
and let it publish to the fork's own GHCR and Releases.

### One-time GitHub settings

- After the first publish, set both GHCR packages (`pythia/pythia-backend`,
  `pythia/pythia-frontend`) to **public**: package page → Package settings →
  Change visibility. The `org.opencontainers.image.source` label links them to
  this repository; check the package page shows that link. If the first push is
  refused with 403, the owner's package settings must grant this repository
  write access (Package settings → Manage Actions access).
- Enable **immutable releases** for the repository if the plan offers it
  (Settings → General → Releases), so a published release's assets cannot be
  replaced afterwards. The `.sha256` asset detects a corrupted download, not a
  replaced pair; immutability is what covers that.
- Protect the tags with a ruleset for `v*` (Settings → Rules → Rulesets:
  restrict deletions and updates).

### Tags are immutable

A wrongly tagged commit gets the **next patch version**. A release tag is never
moved or deleted: an image and a release named after it would otherwise stand
for two different builds.

## Images are pinned by digest

The staging and production overlays pin both images by **digest**
(`${REGISTRY}/pythia/pythia-backend@${BACKEND_DIGEST}`), never by tag. The
overlays set `imagePullPolicy: IfNotPresent`, so a floating tag would silently
keep serving whatever image is already on the node — the deploy would report
success and change nothing. A digest cannot move, so the deployed image is
exactly the one the release published, and a rollback is just the previous
release's bundle.

## Deploying by hand

`scripts/deploy.sh` is the same code the pipeline runs. From a downloaded
release:

```bash
V=v1.2.0
sha256sum -c pythia-deploy-$V.tar.gz.sha256
tar -xzf pythia-deploy-$V.tar.gz && cd pythia-deploy-$V
export REGISTRY=... INGRESS_HOST=...
export OIDC_AUTH_SERVER_URL=... OIDC_CLIENT_ID=...    # and the other stage variables
./scripts/deploy.sh staging      # or: production
./scripts/smoke.sh https://$INGRESS_HOST
```

`deploy.sh` reads the bundle's `release.env` first, before it contacts any
cluster. It **parses** the file and never sources it, because a bundle is
downloaded data: every line must be `KEY=value` with one of the six documented
keys, each exactly once, all six present, and both digests of the form
`sha256:<64 hex>`. If you also exported `BACKEND_DIGEST` or `FRONTEND_DIGEST`
with a DIFFERENT value, it refuses rather than letting either side win silently.

From a **git checkout** (testing against a local minikube) there is no
`release.env`, so export `BACKEND_DIGEST` and `FRONTEND_DIGEST` yourself.

Either way it then renders the overlay, discovers which `${...}` placeholders
the manifests actually contain, refuses to run if any of them is unset — naming
the missing ones — refuses a malformed `*_DIGEST`, then substitutes with
`envsubst` and applies, and finally blocks on `kubectl rollout status` for both
deployments. The unset-variable check matters because `envsubst` replaces an
unset variable with the empty string: without it a missing `REGISTRY` would
deploy an image reference with no registry rather than failing.

## Rollback

Deploy the **previous release's bundle** through the same script:

```bash
cd pythia-deploy-<previous-version>
REGISTRY=... INGRESS_HOST=... OIDC_AUTH_SERVER_URL=... OIDC_CLIENT_ID=... \
  ./scripts/deploy.sh production
./scripts/smoke.sh https://<host>
```

The bundle carries the manifests AND the image digests of one release, so a
rollback cannot pair today's manifests with yesterday's image.

**A rollback does not undo a Flyway migration.** Migrations are forward-only:
the backend runs `QUARKUS_FLYWAY_MIGRATE_AT_START=true`, so a release's schema
changes are applied the moment it starts. Rolling back across a release that
added a migration is **not safe** — the older backend meets a schema it does not
know. Check the releases in between for new `V*.sql` files first; if there are
any, restore from a dump taken before the upgrade (below) instead of rolling the
image back.

## The server itself

minikube is a development tool being used as the production runtime. That is the
given constraint; these are the consequences it forces you to handle.

### minikube must start at boot

Nothing restarts it after a reboot. Install a systemd unit:

```ini
# /etc/systemd/system/minikube.service
[Unit]
Description=minikube cluster hosting Pythia
After=network-online.target docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
User=<deploy-user>
ExecStart=/usr/local/bin/minikube start --profile <profile> --driver=docker
ExecStop=/usr/local/bin/minikube stop --profile <profile>
TimeoutStartSec=600

[Install]
WantedBy=multi-user.target
```

`systemctl enable --now minikube.service`. Use the **same profile and driver**
the cluster was created with; a different driver silently creates a second,
empty cluster — with none of your data in it.

### Ingress

Enable the addon once: `minikube addons enable ingress --profile <profile>`.
Nothing forwards the host's :80/:443 into the cluster by default — route them to
the ingress controller (`minikube ip` plus nftables/iptables DNAT, or a small
nginx reverse proxy on the host). Verify after every reboot; the minikube IP can
change when the cluster is recreated.

### TLS

`k8s/overlays/production/ingress-tls.yaml` expects a secret named
`estimation-tls` in the production namespace, holding the certificate for
`${INGRESS_HOST}`. Create it from the corporate PKI certificate:

```bash
kubectl -n estimation create secret tls estimation-tls \
  --cert=fullchain.pem --key=privkey.pem
```

Renewal is manual unless the corporate PKI issues short-lived certificates and
you automate it — put a calendar reminder ahead of expiry. Re-running the same
command with `--dry-run=client -o yaml | kubectl apply -f -` replaces it in
place; no redeploy is needed.

### Registry pull secret

Needed only when `REGISTRY` requires credentials — the public GHCR images need
none. The manifests reference no credentials — create the secret on the server,
and attach it to each namespace's `default` ServiceAccount so no Deployment
needs patching:

```bash
for ns in estimation estimation-staging; do
  kubectl -n "$ns" create secret docker-registry regcred \
    --docker-server="$REGISTRY" \
    --docker-username="$REGISTRY_USER" \
    --docker-password="$REGISTRY_PASSWORD"
  kubectl -n "$ns" patch serviceaccount default \
    -p '{"imagePullSecrets":[{"name":"regcred"}]}'
done
```

Never commit a `dockerconfigjson` manifest. `k8s/base/postgres/secret.yaml` is a
dev-only sample and must not become the precedent.

### Node sizing

Both namespaces share one node. Production requests 2 × (512Mi, 500m) for the
backend and scales to 3 under load; staging is pinned to 1 replica with its HPA
capped at 1 for exactly this reason. Each namespace also runs a PostgreSQL
(256Mi/250m requested, 512Mi/1 CPU limit) and a frontend. Start the cluster with
headroom over the sum:

```bash
minikube start --profile <profile> --cpus=4 --memory=8g
```

Under-provisioning shows up as a staging rollout evicting a production pod — the
failure mode this sizing exists to prevent.

## Backups — read this before anything else

**`minikube delete` destroys the production database.** PostgreSQL is a
StatefulSet on a 1Gi PVC provisioned inside the minikube VM; deleting the
cluster deletes the volume with it. There is no other copy unless you make one.

Schedule a dump **off the node**, e.g. a nightly cron on the host:

```bash
kubectl -n estimation exec statefulset/postgres -- \
  pg_dump -U estimation estimation | gzip > "/var/backups/pythia/$(date +%F).sql.gz"
```

Keep the backups on a filesystem that is itself backed up, and prune old ones.
An untested backup is not a backup: restore into a scratch namespace
periodically and confirm the row counts.

Restore:

```bash
gunzip -c /var/backups/pythia/<date>.sql.gz \
  | kubectl -n estimation exec -i statefulset/postgres -- psql -U estimation estimation
```

Scale the backend to zero first (`kubectl -n estimation scale deploy/backend
--replicas=0`) so nothing writes mid-restore, and scale it back afterwards.

## Reaching staging

Staging needs its own hostname; a subdomain of the production one is the obvious
choice, supplied as `INGRESS_HOST` when deploying the staging environment.

If a second DNS record is not available, do **not** invent a fake host. Deploy
staging with the ingress patch removed from
`k8s/overlays/staging/kustomization.yaml` and reach it through the SSH tunnel
instead:

```bash
ssh -L 8081:localhost:8081 "$DEPLOY_HOST"
kubectl -n estimation-staging port-forward svc/frontend 8081:80
```

## Related documents

- `docs/development.md` — local development, the build, and the GitHub CI pipeline
- `docs/architecture.md` — how the modules fit together
- `docs/authentication.md` — the pluggable auth providers and what OIDC expects
