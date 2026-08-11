package io.pythia.observability

import io.quarkus.logging.Log
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.slf4j.MDC
import java.util.UUID

// Per-request correlation id (task-026). Reads `X-Correlation-ID` from the
// incoming request (or generates a UUID when absent), puts it into the logging
// MDC under `correlationId` so every JSON log line for the request carries it
// (quarkus-logging-json serialises the MDC), echoes it back on the response
// `X-Correlation-ID` header, and clears the MDC entry afterwards so pooled worker
// threads do not leak it. Runs before the auth filters so their logs already
// carry the id.
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
@ApplicationScoped
class CorrelationIdFilter : ContainerRequestFilter, ContainerResponseFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        val incoming = requestContext.getHeaderString(HEADER)?.takeIf { it.isNotBlank() }
        val correlationId = incoming ?: UUID.randomUUID().toString().also {
            Log.debug("Generated correlation id $it for ${requestContext.method} ${requestContext.uriInfo.path}")
        }
        requestContext.setProperty(PROPERTY, correlationId)
        MDC.put(MDC_KEY, correlationId)
    }

    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        (requestContext.getProperty(PROPERTY) as? String)?.let {
            responseContext.headers.putSingle(HEADER, it)
        }
        MDC.remove(MDC_KEY)
    }

    private companion object {
        private const val HEADER = "X-Correlation-ID"
        private const val PROPERTY = "io.pythia.correlationId"
        private const val MDC_KEY = "correlationId"
    }
}
