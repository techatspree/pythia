# The MCP interface

Pythia can give AI assistants (Claude, an IDE agent, any client of the
[Model Context Protocol](https://modelcontextprotocol.io)) **read-only** access
to its projects, estimations and versions. An assistant can then answer
questions such as "what did we estimate for this offer, and what changed since
version 2?" from the same numbers the web UI shows.

The interface is **off by default**. An administrator switches it on for the
whole installation.

## What it exposes

Six tools. Each returns, as JSON, exactly what the corresponding REST endpoint
returns: no number is computed for the AI, it sees what the UI sees.

| tool               | arguments                                         | returns                              | REST twin |
|--------------------|---------------------------------------------------|--------------------------------------|-----------|
| `list_projects`    | `status` (optional): `ACTIVE` or `ARCHIVED`       | the projects                         | `GET /api/projects` |
| `get_project`      | `projectId`                                       | one project with its estimations     | `GET /api/projects/{id}` |
| `get_estimation`   | `estimationId`                                    | one estimation with its draft total  | `GET /api/estimations/{id}` |
| `list_versions`    | `estimationId`                                    | the versions, draft first            | `GET /api/estimations/{id}/versions` |
| `get_version`      | `estimationId`, `version` (`"draft"` or a number) | one version with all calculated values | `GET …/versions/draft`, `GET …/versions/{n}` |
| `compare_versions` | `estimationId`, `versionA`, `versionB`            | the comparison                       | `GET …/versions/{a}/compare/{b}` |

There are no write tools: changing an estimate needs the provisioning, audit
and undo the web UI's write endpoints carry, which is a design of its own.
An unknown id or version is an error result with the same message as the REST
`404`.

## Switching it on

As an administrator, open **Systemeinstellungen** (`/admin/system`), tick
**MCP-Schnittstelle aktivieren** in the section **KI-Schnittstelle (MCP)**, and
save. The page then shows the endpoint. The switch takes effect at once.

While it is off, `/api/mcp` answers `404` to every signed-in caller: the
interface is absent, not empty.

The same switch through the API (admin only):

    curl -X PUT https://<your-host>/api/system/mcp \
         -H "Authorization: Bearer <admin-token>" \
         -H "Content-Type: application/json" -d '{"enabled": true}'

## The endpoint and authentication

The endpoint is `https://<your-host>/api/mcp` (MCP **Streamable HTTP**). It sits
under `/api/`, so it needs no extra gateway or ingress configuration.

A client authenticates **exactly like every other API client of the
installation**; there is no separate MCP credential:

- **Entra ID** (production): an OAuth 2.0 bearer access token for the Pythia
  API, the same audience the web UI requests (see
  [Entra ID setup](entra-setup.md)).
- **Dev module** (local development): the header
  `Authorization: Dev dev-viewer` (or any other dev user).

The caller needs the **VIEWER** role, like every read. An anonymous request is
`401`, whether the interface is on or off.

An example registration in Claude Code:

    claude mcp add --transport http pythia https://<your-host>/api/mcp \
        --header "Authorization: Bearer <token>"

## Not offered yet: automatic sign-in

MCP clients can discover how to sign in on their own (OAuth protected-resource
metadata, RFC 9728). Pythia does not serve that metadata yet: it would live under
`/.well-known/…`, outside the `/api/` path the gateway forwards, and it needs
the installation's external URL, a per-stage setting. Entra ID also offers no
dynamic client registration, so an automatic sign-in could not complete anyway.
Configure the bearer token (or a pre-registered client) in your MCP client
yourself.

## The DNS-rebinding check

The MCP extension refuses requests whose `Origin` header names another host
(`403`), but only while the server listens on `localhost`, i.e. in a local
`quarkus dev`. It protects a developer's machine from a web page that tries to
reach the local server. In the cluster the server listens on all interfaces
and the check is off. MCP clients send no `Origin` header, so they are never
affected.
