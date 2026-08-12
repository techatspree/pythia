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
- Container images are pulled from the **company's internal registry**.

```
GitHub (public)                 internal GitLab                the server
─────────────────               ───────────────                ──────────
push to main  ──pull mirror──▶  pipeline runs
                                  ├─ build + push images ──▶  internal registry
                                  ├─ deploy staging ────ssh──▶ minikube
                                  │                             ns estimation-staging
                                  ├─ ⏸ manual approval
                                  └─ deploy production ─ssh──▶  ns estimation
```

### Why the pipeline is not in this repository

CI — build, test, Playwright, image build — runs on **GitHub Actions**
(`.github/workflows/ci.yaml`), because that is where pull requests land and it
must be visible to outside contributors. It holds no secrets and deploys
nothing.

Deployment cannot run there: a GitHub-hosted runner cannot reach a private
corporate network. It runs on an internal GitLab runner instead, and the
pipeline definition itself is kept out of this repository so that internal
coordinates never enter a public git history.

## GitLab setup

**1. Pull mirror.** In the internal GitLab project: *Settings → Repository →
Mirroring repositories*, direction **Pull**, pointing at
`https://github.com/techatspree/pythia.git`. The GitLab copy is a read-only
mirror; pipelines still run on it normally.

**2. External CI configuration.** *Settings → CI/CD → General pipelines → CI/CD
configuration file*, set to a path in another project, e.g.
`.gitlab-ci.yml@<internal-group>/pythia-deploy`. This is what keeps
`.gitlab-ci.yml` out of the public repository entirely. What stays here is the
generic, reviewable deployment *logic* — `scripts/deploy.sh` and
`scripts/smoke.sh` — which is parameterised purely by environment variables.

**3. CI/CD variables.** All masked; all protected on the production job.
Descriptions only — never record a value in this file.

| Variable | Holds |
|---|---|
| `REGISTRY` | Host (and optional path prefix) of the internal container registry. |
| `IMAGE_TAG` | The short commit SHA being deployed. Never a floating tag — see below. |
| `INGRESS_HOST` | Public hostname for the environment being deployed. |
| `OIDC_AUTH_SERVER_URL` | Issuer URL of the identity provider realm. |
| `OIDC_CLIENT_ID` | Client id registered for that environment. |
| `DEPLOY_HOST` | SSH target of the server (`user@host`). |
| `DEPLOY_SSH_KEY` | Private key for that SSH user. File-type variable. |
| `REGISTRY_USER` / `REGISTRY_PASSWORD` | Credentials for the image pull secret and for `docker login` on the runner. |
| `SMOKE_TOKEN` | Optional bearer token so the smoke test can assert an authenticated 200 rather than a 401. |

**4. Runner requirements.** Docker, plus access to the corporate artifact
mirror. The build resolves from Maven Central and the npm registry, and the Jib
base image is pinned by digest to `registry.access.redhat.com/ubi9/openjdk-21-runtime`
— in a private network all three must be proxied, or the pipeline fails at
dependency resolution long before it reaches a deploy.

## Pipeline stages

1. **Build and push images.** Reuse the existing coordinates; override only the
   registry prefix.

   ```bash
   ./gradlew :backend:implementation:imageBuild \
     -Dquarkus.container-image.build=true \
     -Dquarkus.container-image.registry="$REGISTRY" \
     -Dquarkus.container-image.push=true
   ./gradlew :frontend:dockerBuildImage
   docker tag  pythia/pythia-frontend:1.0.0-SNAPSHOT "$REGISTRY/pythia/pythia-frontend:$IMAGE_TAG"
   docker push "$REGISTRY/pythia/pythia-frontend:$IMAGE_TAG"
   ```

   Do **not** change the reproducibility settings while doing so: both
   `quarkus.jib.use-current-timestamp*` stay `false`, and no time- or
   environment-derived jar-manifest attribute may be added. The GitHub `images`
   job asserts the backend image is byte-identical across rebuilds and will go
   red if that is broken.

2. **Deploy staging** — automatic on `main`.
3. **Manual approval** — a GitLab job with `when: manual`, on a protected
   environment so only authorised users can trigger it.
4. **Deploy production.**
5. **Smoke test** after each rollout.
6. **Tag the commit** on a successful production deploy, so the running version
   is identifiable from the repository.

Each deploy step SSHes to the server and runs the deployment *there*, where the
kubeconfig and the cluster are local:

```bash
ssh "$DEPLOY_HOST" "cd /opt/pythia && git fetch --all && git checkout $CI_COMMIT_SHA \
  && REGISTRY='$REGISTRY' IMAGE_TAG='$CI_COMMIT_SHORT_SHA' INGRESS_HOST='$INGRESS_HOST' \
     OIDC_AUTH_SERVER_URL='$OIDC_AUTH_SERVER_URL' OIDC_CLIENT_ID='$OIDC_CLIENT_ID' \
     ./scripts/deploy.sh production"
ssh "$DEPLOY_HOST" "cd /opt/pythia && ./scripts/smoke.sh https://$INGRESS_HOST"
```

The server keeps a checkout of the mirror at a fixed path (`/opt/pythia` above),
and the pipeline advances it to the exact commit being deployed — so the applied
manifests and the deployed images provably come from the same commit.

**Do not expose the minikube API server to the runner.** Its endpoint is the
driver's internal IP; publishing it means certificate SANs and a firewall hole,
and buys nothing over SSH.

## Image tags: always immutable

Push every build under the short commit SHA and deploy *that*. The overlays set
`imagePullPolicy: IfNotPresent`, so a floating tag like `1.0.0-SNAPSHOT` would
silently keep serving whatever image is already on the node — the deploy would
report success and change nothing. An immutable tag also makes rollback trivial.

## Deploying by hand

`scripts/deploy.sh` is the same code the pipeline runs, and works against a
local minikube:

```bash
export REGISTRY=... IMAGE_TAG=... INGRESS_HOST=...
export OIDC_AUTH_SERVER_URL=... OIDC_CLIENT_ID=...
./scripts/deploy.sh staging      # or: production
./scripts/smoke.sh https://$INGRESS_HOST
```

It renders the overlay, discovers which `${...}` placeholders the manifests
actually contain, refuses to run if any of them is unset — naming the missing
ones — then substitutes with `envsubst` and applies, and finally blocks on
`kubectl rollout status` for both deployments. The unset-variable check matters
because `envsubst` replaces an unset variable with the empty string: without it
a missing `REGISTRY` would deploy an image reference with no registry rather
than failing.

## Rollback

Redeploy the previous immutable tag through the same script:

```bash
ssh "$DEPLOY_HOST" "cd /opt/pythia && git checkout <previous-sha> \
  && REGISTRY=... IMAGE_TAG=<previous-short-sha> INGRESS_HOST=... \
     OIDC_AUTH_SERVER_URL=... OIDC_CLIENT_ID=... ./scripts/deploy.sh production"
./scripts/smoke.sh https://<host>
```

Check out the matching commit as well as the matching tag: a rollback that
reuses today's manifests with yesterday's image can reintroduce the very
mismatch you are backing out of.

**A rollback does not undo a Flyway migration.** The backend runs
`QUARKUS_FLYWAY_MIGRATE_AT_START=true`, so schema changes have already been
applied. If the release contained a destructive migration, restore from a dump
(below) instead of rolling the image back.

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

The manifests reference no credentials — create the secret on the server, and
attach it to each namespace's `default` ServiceAccount so no Deployment needs
patching:

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
