# MCP server (task-218)

Loads when working under `io.pythia.mcp`. The user-facing description is
`docs/mcp.md`; the backend-wide rules are in `src/backend/CLAUDE.md`.

A read-only **Model Context Protocol** server over Streamable HTTP at
`/api/mcp` (`quarkus.mcp.server.http.root-path`), built on the Quarkiverse
extension `quarkus-mcp-server-http`. Two classes:

- `PythiaMcpTools` — the six `@Tool`s.
- `McpGate` — the per-tenant switch in front of them.

## The extension's version comes from the platform

`build.gradle.kts` imports `io.quarkus.platform:quarkus-mcp-server-bom` at
`quarkusPlatformVersion` and declares the extension WITHOUT a version (3.40.1
pins 2.0.1). Do not hand-pick a Quarkiverse version: 2.0.4 is built against
Quarkus 3.33. A platform upgrade moves the extension with it. The same BOM
manages `quarkus-mcp-server-test` (`McpAssured`), used by `McpServerIT`.

## The gate: per request, 404 when off, after authentication

`system_settings.mcp_enabled` (default false) is read on EVERY request to
`/api/mcp` and `/api/mcp/*`, never cached, so an admin's switch is immediate
with any number of pods. Off answers `404` with the structured
`{ "message", "status" }` body: the interface is absent, not empty.

- **Authentication runs first.** `quarkus.http.auth.permission.mcp.*` makes the
  path `authenticated`; the security filters run at negative route orders, so
  an anonymous caller is `401` with the switch on or off and only a signed-in
  caller ever sees the 404.
- **The gate's order is measured, not guessed.** The extension registers its
  endpoint as a framework route without an explicit order, and Vert.x numbers
  such routes from 0. A gate at `ROUTE_ORDER_BEFORE_DEFAULT` (1000) ran AFTER
  the extension and never fired; `-1` runs after the security filters and
  before every unordered route. `McpServerIT` cases "off by default…" and
  "switching off again…" fail if the order breaks.
- The request is paused while the switch is read off the event loop, so the
  extension's body handler still gets the whole JSON-RPC body.

## Tools read `SecurityIdentity`, never `CurrentUserProvider`

`CurrentUserProvider` is filled by the JAX-RS filters (`EntraAuthFilter`,
`DevAuthFilter`). An MCP request is a Vert.x route, not JAX-RS, so it is
EMPTY here. `SecurityIdentity` is populated by the authentication mechanism
itself and works on both paths.

## Same DTO as the REST twin, no computation

Each tool returns the DTO its REST endpoint returns, serialised with the
application's `ObjectMapper`, through the existing mappers and services
(`EstimationVersionService.versionSummaries` / `resolveVersion` are shared
with `EstimationVersionResource`). Nothing is calculated in the tool layer:
the numbers stay in the KMP domain. `canChangeOwner` is always `false`.
Errors are `ToolCallException`s with the REST 404's message.

Each tool is `@RolesAllowed("VIEWER")`, like every REST read. That a caller
without VIEWER gets an error and never data is design intent, not tested:
every dev user has VIEWER.

## Read-only by decision

No write tools, resources or prompts. Writes would need the provisioning, audit
and undo semantics of the mutating REST endpoints, which is a design of its own.

## The DNS-rebinding check stays at its default

The extension's check reads the **`Origin`** header (not `Host`) and is active
only while `quarkus.http.host` is a localhost name: on in `%dev`/`%test`
(`localhost`), off in the container (`0.0.0.0`). A foreign `Origin` gets `403`;
MCP clients send none. `McpServerIT` pins both halves (no `Origin` with a
foreign `Host` → 200; foreign `Origin` → 403 in `%test`).
