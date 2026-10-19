# Backend (`src/backend`) — Quarkus 3

Loaded when working under `src/backend/`. Quarkus 3 (Java 21, Kotlin) via the `io.quarkus` Gradle plugin, Hibernate ORM Panache, Flyway, Jib for container images. REST endpoints under `/api/`. The one-approach-per-module logging rule and the single-source-of-truth rule are in the root `CLAUDE.md`.

## Detail next to the code

Package detail lives in a `CLAUDE.md` in the package (under `implementation/src/main/kotlin/io/pythia/`) and loads when you work there. Read it before changing that area from outside.

- `domain/CLAUDE.md` — persistence shape: the draft/submitted node tables, buckets, schedule inputs, the undo log, and the constraint traps behind them.
- `rest/CLAUDE.md` — endpoint behaviour the OpenAPI document does not carry (versions, project ownership, the auth endpoints) and the dev-only `DELETE /api/dev/test-data`.
- `service/CLAUDE.md` — Excel/CSV export and import: method-driven columns, the frozen column positions, the `Eimer` sheet, the `422` on a method mismatch.
- `auth/CLAUDE.md` — the provider-modular auth SPI, roles, the fail-closed dev module, JIT user provisioning, the per-user language.

## Migration ↔ entity parity (applies to every schema change)

`%test` and `%dev` build the schema from Hibernate (`drop-and-create`, Flyway off); only prod and dev-minikube run the Flyway migrations. So **anything a migration adds must also be declared where Hibernate sees it**, or it does not exist where you test: a constraint goes on the entity too (`@Table(uniqueConstraints/check = …)`), and a row a migration seeds needs a bootstrap that creates it (`SystemSettingsBootstrap`). Both have bitten: a bucket-reorder bug was unreproducible outside the cluster, and the dev backend once failed to boot on a missing settings row. Detail in `domain/CLAUDE.md` and "System settings" below.

## Logging

Log via the static `io.quarkus.logging.Log` API (`Log.info/debug/warn/error`) — **no per-class logger field**. Levels are configured per profile in `application.properties`: app code (`io.pythia`) at DEBUG in `%dev`/`%test`, INFO in `%prod`; the root level stays at the Quarkus default. The PROD profile emits structured JSON (`%prod.quarkus.log.console.json.enabled=true`, backed by `quarkus-logging-json`) for Kubernetes; the human-readable console stays the default in dev. `EstimationVersionService` is the reference site (INFO on create/submit/delete with the estimation id, DEBUG for finer detail, ERROR on failure paths).

Every request carries a **correlation id** (task-026): `CorrelationIdFilter` (`io.pythia.observability`, a JAX-RS request+response `@Provider`) reads the `X-Correlation-ID` header (or generates a UUID), puts it in the logging MDC under `correlationId` — so the JSON logs carry it per request — and echoes it back on the response header. Prometheus metrics are served at `/q/metrics` (Micrometer, `http_server_requests_*` timers) via `quarkus-micrometer-registry-prometheus`.

## Run-of-the-mill patterns

- Domain → entity mapping in `…/service/DraftVersionMapper.kt`; entity → DTO mapping in `…/rest/dto/EstimationVersionMapper.kt`; entity → `DraftUpdateDto` snapshot in `…/service/DraftSnapshotMapper.kt` (`DraftEstimationVersion.toUpdateDto()`).
- Writing a `DraftUpdateDto` into a draft entity is centralised in `…/service/DraftUpdateApplier.kt` (`apply(draft, update)`) — the `dto.field?.let { … list.clear(); list.add(…) }` clear-and-rebuild shape (rather than diffing). Both the REST PUT `updateDraft` and the Undo service call it; `toUpdateDto()` is its inverse (capture), so `apply(draft, draft.toUpdateDto())` is a no-op on state.
- Repositories are Panache (`PanacheRepository<T>`). Add finders as extension methods on the repository class.
- Use `@SQLRestriction`, not the deprecated `@Where`, for filtered collections.

## REST API surface

The full contract — every path, request/response schema, and status code — is the committed `src/frontend/src/lib/api/openapi.json`; read it rather than relying on a prose list here.

Every JSON endpoint documents its **request and response** schemas plus real status codes (401/403/404/409/204) in the generated OpenAPI document (task-094). Authorization failures carry a structured JSON body `{ "message": …, "status": … }` via `ForbiddenExceptionMapper` (403) / `UnauthorizedExceptionMapper` (401) in `rest/error/AuthorizationExceptionMappers.kt` (task-093) — user `ExceptionMapper`s that override Quarkus' built-in defaults for `io.quarkus.security.{Forbidden,Unauthorized}Exception`, logging the denial (WARN/INFO) with the request path. The JAX-RS methods keep their `jakarta.ws.rs.core.Response` return type; response bodies are declared with MicroProfile OpenAPI annotations (`@APIResponse` + `@Content`/`@Schema`, `@Tag`, `@Operation`).

SmallRye writes the document to `src/backend/implementation/target/openapi/openapi.json` during augmentation (`quarkus.smallrye-openapi.store-schema-directory`), and **the Gradle build carries it into the frontend** (task-114): `:backend:implementation` snapshots it to `build/openapi/` and publishes it through the consumable `openapiSchema` configuration; `:frontend` resolves it via `backendOpenapi`, and `syncBackendOpenapi` refreshes the committed `src/frontend/src/lib/api/openapi.json` before `genApi` regenerates `schema.d.ts`. So `./gradlew build` keeps both files current — **commit them with the change that caused them**; no manual `cp`/`npm run gen:api` step.

Two traps the wiring works around, do not undo them: the document is written as a SIDE EFFECT of augmentation into a path no Quarkus task declares as an output, so `quarkusAppPartsBuild` gets an explicit `outputs.file(...)` or a cached/up-to-date augmentation silently leaves it missing; and BOTH the prod augmentation (`%prod` → OIDC, emits `securitySchemes`) and the `@QuarkusTest` augmentation (`%test` → dev auth, does not) write that same path, so `test` is ordered `mustRunAfter` the snapshot to pin the **prod** contract — the one that describes the deployed API.

The document is also pinned to be **environment-independent** (task-135): `quarkus.smallrye-openapi.security-scheme=oidc` + `quarkus.smallrye-openapi.oidc-open-id-connect-url=https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration` in `application.properties`. Without them Quarkus derives the documented `openIdConnectUrl` from the OIDC config resolved at augmentation (`%prod.quarkus.oidc.auth-server-url`, which interpolates `${ENTRA_TENANT_ID}`), so the committed schema — and therefore the drift gate — depended on whether the building shell had that variable set, and a real tenant UUID was committed as build output. **Both** properties are required: the URL alone is only a fallback that auto-detection overrides whenever the live OIDC config resolves; `security-scheme=oidc` selects the explicit path so the pin wins. This is documentation-only and does NOT change runtime OIDC.

**The document is linted, and the build fails on a finding** (task-204): `:frontend:lintOpenApi` runs Redocly's `recommended-strict` ruleset, which makes EVERY rule blocking. A new endpoint must therefore declare an `operationId` (its method name, see below), its security (from `@RolesAllowed`/`@Authenticated`, or an empty `@SecurityRequirements` for a deliberately public read, task-202) and at least one 4xx response. A public read that genuinely cannot return a 4xx gets an entry with its reason in `src/frontend/.redocly.lint-ignore.yaml`; never switch a rule off in `redocly-lint.yaml`.

**Every operation has a generated, unique `operationId`** (task-201): `quarkus.smallrye-openapi.operation-id-strategy=METHOD` takes it from the Kotlin method name, and `mp.openapi.extensions.smallrye.duplicateOperationIdBehavior=FAIL` makes a collision fail augmentation (`SROAP07950: Duplicate operationId`) instead of only warning. METHOD rather than CLASS_METHOD keeps the resource class out of the client's names, so **resource methods are named verb–noun and must be unique ACROSS all resources** — `getSession`, `getSystemSettings`, never a bare `get`. Do not write `@Operation(operationId = …)`: the method name is the one place an operation is named. `OpenApiOperationIdIT` pins the served document (present, unique, camelCase). With ids present, `schema.d.ts` carries a real `operations` map instead of `Record<string, never>`.

The drift gate `:frontend:verifyOpenApiSchemaCommitted` (`git diff --exit-code --stat HEAD`, so `git add`-ing a stale schema cannot satisfy it) is **opt-in**: it joins `check` only when `CI` is set or `-PopenApiGate` is passed. That is deliberate — wiring it in unconditionally would fail every local build that touches a REST DTO until the regenerated files were committed, while the task workflow requires a green build *before* the commit. (Frontend component code still consumes the hand-written `$lib/api/types.ts`; migrating it onto the generated `schema.d.ts` is a separate follow-up.)

## Configuration: stage coordinates are not in the image

**A stage differs from another stage by its configuration, never by its image**
(task-161). `%prod` in `application.properties` therefore holds only
**stage-invariant** values — `quarkus.datasource.db-kind`,
`hibernate-orm.schema-management.strategy`, `flyway.migrate-at-start`,
`oidc.application-type`, `oidc.roles.role-claim-path`, the JSON logging flag.
Everything that varies per stage arrives as an environment variable from the
`backend-config` ConfigMap (or the `postgres-credentials` Secret for
credentials): `APP_AUTH_PROVIDER`, `QUARKUS_DATASOURCE_JDBC_URL`,
`QUARKUS_DATASOURCE_USERNAME`/`_PASSWORD`, `QUARKUS_OIDC_AUTH_SERVER_URL`,
`QUARKUS_OIDC_CLIENT_ID`, `QUARKUS_OIDC_TOKEN_AUDIENCE`. No application code
reads those names — Quarkus maps each back to its property through MicroProfile
Config. `docs/deployment.md` carries the key table and the `${OIDC_*}`
placeholder convention every overlay shares.

**No `%prod` property may contain a `${...}` placeholder.** Expanding one from
the pod environment is a *second* configuration mechanism, and having both is
exactly how production came to reference an `ENTRA_API_CLIENT_ID` that no
production manifest supplied while `QUARKUS_OIDC_TOKEN_AUDIENCE` went unset.
One mechanism: an env var from the ConfigMap.

**Some properties cannot leave the image, and a container that starts is not
proof that one did.** Quarkus fixes a subset at augmentation (build) time;
`quarkus.datasource.db-kind` is the obvious one. The trap worth knowing:
`quarkus.oidc.enabled` is build-time too (`OidcBuildTimeConfig`), so the
`QUARKUS_OIDC_ENABLED` key in `backend-config` is **inert at runtime** — it
cannot switch OIDC on or off in a built image. The runtime lever is
`quarkus.oidc.tenant-enabled`. Before moving a property out, confirm it is a
runtime one and prove the env var changes behaviour.

`AuthProviderGuard` logs what actually resolved — the active provider at INFO,
the issuer / client id / audience at DEBUG — so a mis-set ConfigMap key surfaces
in the pod log rather than as an unexplained 401. Those three are public
identifiers; the datasource password is not, and is never logged.

## Reproducible Jib backend image

`src/backend/implementation/src/main/resources/application.properties` configures Jib for byte-deterministic images (container-image name, digest-pinned base, platforms, timestamp flags). They lived in the module's `gradle.properties` until task-198: **Quarkus 3.40's Gradle plugin no longer reads `quarkus.*` keys from a sub-project's `gradle.properties`** and silently fell back to defaults (image `<user>/implementation`, a floating base, current timestamps). Never move them back. **Both** `quarkus.jib.use-current-timestamp` and `quarkus.jib.use-current-timestamp-file-modification` must be `false`. Setting only one yields non-reproducible builds.

**Reproducible inputs, not byte-identical output.** Quarkus' augmentation emits one non-deterministic artifact regardless of these settings: `quarkus/generated-bytecode.jar`, specifically `io/quarkus/runner/recorded/ResteasyReactiveProcessor$setupDeployment*.class` — 1 of 971 entries, measured by building twice and diffing. Everything else in `build/quarkus-app/` (260 files) is identical, and the application jar itself is reproducible. Never write a check that assumes byte-identical images; CI asserts the inputs instead (`docs/deployment.md`).

## System settings (per installation)

Per-installation configuration (task-146) — the operating organisation's **display name**, its **standard effort drivers**, and an optional **custom stylesheet**. This is installation DATA, not business logic: none of it lives in the KMP domain, and it is deliberately kept away from `EstimationDefaults` (which holds universal calculation defaults).

`system_settings` (`V18`) is a **singleton**: `id SMALLINT PRIMARY KEY DEFAULT 1` with `CHECK (id = 1)`, so a second row is unrepresentable. The CHECK is mirrored on the `SystemSettings` entity via `@Table(check = [CheckConstraint(...)])` (`jakarta.persistence`, valid since Jakarta Persistence 3.2 — this project is on 3.2.0/Hibernate 7.3.2) per the migration ↔ entity parity rule above. `SystemSettings` deliberately does **not** extend `BaseEntity`: it has a fixed `Short` id rather than a generated uuid.

**The seeded ROW needs the same parity treatment as a constraint, and that is the trap.** `V18` INSERTs the singleton row, but `%test`/`%dev` build the schema from Hibernate with Flyway OFF, so that INSERT never runs there and every read 500s. `SystemSettingsBootstrap` (`@Observes StartupEvent`, mirroring `AuthProviderGuard`/`MethodRegistryBootstrap`) creates the row when absent, in every profile. When a migration seeds data the tests depend on, mirror the DATA too — not just the constraints. **Since task-213 it must also run FIRST:** `TestDataSeeder` (`@Priority(1)`) submits drafts at startup, and submitting reads the hours per working day from this row, so the bootstrap observes at `@Priority(Interceptor.Priority.PLATFORM_BEFORE)`; without it the dev backend failed to boot with "system_settings row 1 is missing".

**Hours of a working day (task-213).** `system_settings.hours_per_day` (`V21`, `NOT NULL DEFAULT 8`, `CHECK (> 0 AND <= 24)`, mirrored on the entity) divides accompanying work's hours into person-days. It is set through its OWN endpoint, `PUT /api/system/hours-per-day` (ADMIN, `204`/`400`/`403`), not as a field of `PUT /api/system`: there a null `displayName` means "clear the name", so an hours-only PUT would wipe it. `GET /api/system` (public) returns it. `DraftVersionMapper.toDomain(draft, hoursPerDay)` takes it as a parameter rather than injecting the service, so the mapper stays pure; the two callers that CALCULATE (`EstimationVersionService.calculateDraft` and `snapshotDraft`) pass the setting, the diff-only callers (undo, sessions) may keep the default. **Each submitted version records the value it was calculated with** (`submitted_estimation_versions.hours_per_day`, nullable): existing rows stay NULL, because they were calculated BEFORE the conversion with hours counted as person-days, so stamping 8 on them would be false. `EstimationVersionDto.hoursPerDay` is the setting for a draft and the stored value (or null) for a submitted version; `VersionComparisonService` reports a difference as an ordinary `hoursPerDay` parameter change (comparing against the draft goes through `snapshotDraft`, so the draft side carries the current setting); the Excel "Parameter" sheet carries a `Stunden pro Arbeitstag` row (omitted for NULL), which `ExcelImporter.importParameters` ignores like any unknown row — an imported draft follows the tenant setting.

`system_effort_drivers` is the organisation-wide driver template, ordered by a `UNIQUE` `position`. `EstimationVersionService.createDraft` copies it into a new draft **only on the `latestSubmitted == null` branch** — a draft cloned from a submitted version keeps exactly the drivers it inherited, because appending the template there would silently mutate a continuing estimate. Writes replace the whole list (the `DraftUpdateApplier` clear-and-rebuild convention), with `position` taken from the list index.

`SystemSettingsResource` (`/api/system`): `GET /api/system` and `GET /api/system/css` are **`@PermitAll`** — the auth gate renders the brand lockup with the login dialog before any account exists, so role-gating them would leave the login screen unbranded; they expose only public branding. Every write is `@RolesAllowed("ADMIN")` and must stay that way: an uploaded stylesheet cannot run JavaScript but CAN overlay or spoof UI, which is acceptable only at the ADMIN trust level. Uploads are multipart (`@RestForm("file") FileUpload`, the Merlin pattern) and capped at **256 KB**; that is far inside the existing 32m nginx / 32M Quarkus body limits, so **no gateway change is needed** here. `GET /api/system/css` sends `Cache-Control: no-cache` because the browser pulls it in as an ordinary `<link>` stylesheet — without it an admin's change would not appear until a hard reload.
