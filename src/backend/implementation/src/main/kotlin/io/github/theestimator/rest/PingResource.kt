package io.github.theestimator.rest

import jakarta.annotation.security.PermitAll
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

// Unauthenticated liveness heartbeat (task-136). The frontend connection watchdog
// polls this over the existing /api proxy to detect when the backend is reachable
// again after a connection loss. It is @PermitAll on purpose — the whole point is
// to probe reachability even when not signed in. Deliberately NOT logged per call:
// a ~3s poll would spam the log.
@ApplicationScoped
@Path("/api/ping")
@Tag(name = "Health", description = "Liveness heartbeat for the frontend connection watchdog")
class PingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Unauthenticated liveness heartbeat")
    @APIResponse(responseCode = "200", description = "The backend is reachable")
    fun ping(): Response = Response.ok(mapOf("status" to "ok")).build()
}
