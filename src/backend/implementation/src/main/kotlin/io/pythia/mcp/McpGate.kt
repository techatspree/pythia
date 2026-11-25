package io.pythia.mcp

import io.pythia.service.SystemSettingsService
import io.quarkus.logging.Log
import io.quarkus.narayana.jta.QuarkusTransaction
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.util.concurrent.Callable

// The per-tenant switch of the MCP interface (task-218): while
// system_settings.mcp_enabled is false, every request to /api/mcp and
// /api/mcp/* is answered 404 before the MCP extension sees it.
//
// - Per request, never cached: an admin's switch takes effect at once, and it
//   stays correct with any number of backend pods (one per stage today).
// - 404, not an empty tool list: "off" means the interface is absent, so a
//   client cannot even open a session, and nothing about it leaks.
// - After authentication: the HTTP permission in application.properties makes
//   the path `authenticated`, and Quarkus' security handlers run before this
//   route, so an anonymous caller gets 401 whether the switch is on or off and
//   only an authenticated one ever sees the 404 (McpServerIT cases 1 and 2).
// - Order, measured (McpServerIT): the MCP extension registers its endpoint as
//   a framework route WITHOUT an explicit order, and Vert.x numbers such routes
//   from 0 upwards; an order of ROUTE_ORDER_BEFORE_DEFAULT (1000) therefore ran
//   AFTER the extension and the gate never fired. Quarkus' security filters run
//   at negative orders (-priority), so -1 sits after authentication and
//   authorization and before every unordered route.
// - The request is paused while the switch is read off the event loop, so the
//   extension's own body handler still sees the whole JSON-RPC body when the
//   gate lets it through.
@ApplicationScoped
class McpGate(private val settings: SystemSettingsService) {

    fun install(@Observes router: Router) {
        listOf(ROOT, "$ROOT/*").forEach { path ->
            router.route(path).order(GATE_ORDER).handler(::handle)
        }
    }

    private fun handle(ctx: RoutingContext) {
        ctx.request().pause()
        ctx.vertx().executeBlocking(
            Callable { QuarkusTransaction.requiringNew().call { settings.mcpEnabled } }
        ).onComplete { result ->
            when {
                result.failed() -> {
                    Log.error("MCP gate: could not read the MCP switch for ${ctx.normalizedPath()}", result.cause())
                    ctx.request().resume()
                    ctx.response().setStatusCode(INTERNAL_ERROR).end()
                }
                result.result() == true -> ctx.next()
                else -> {
                    Log.debug("MCP gate: interface disabled, 404 for ${ctx.normalizedPath()}")
                    ctx.request().resume()
                    ctx.response()
                        .setStatusCode(NOT_FOUND)
                        .putHeader("Content-Type", "application/json")
                        .end("""{"message":"$DISABLED_MESSAGE","status":$NOT_FOUND}""")
                }
            }
        }
    }

    private companion object {
        const val ROOT = "/api/mcp"
        const val GATE_ORDER = -1
        const val NOT_FOUND = 404
        const val INTERNAL_ERROR = 500
        const val DISABLED_MESSAGE = "The MCP interface is disabled for this installation"
    }
}
