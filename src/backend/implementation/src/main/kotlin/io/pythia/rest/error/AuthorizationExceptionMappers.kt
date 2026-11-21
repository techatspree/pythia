package io.pythia.rest.error

import io.quarkus.logging.Log
import io.quarkus.security.ForbiddenException
import io.quarkus.security.UnauthorizedException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

// Small machine-readable body for authorization failures, kept for API clients.
// The SPA does NOT display this message: for 401/403 it shows its own translated
// text (`src/frontend/src/lib/api/errors.ts`, task-225), because these sentences
// are fixed English strings that carry nothing beyond the status. Backend source
// stays English-only, so the body does too.
data class ApiError(val message: String, val status: Int)

// @RolesAllowed denials throw io.quarkus.security.ForbiddenException (403); a
// user ExceptionMapper takes priority over the Quarkus built-in default and
// gives the response a JSON body. WARN per the CLAUDE.md logging convention.
@Provider
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {

    @Context
    lateinit var uriInfo: UriInfo

    override fun toResponse(exception: ForbiddenException): Response {
        Log.warn("Access denied (403) for ${uriInfo.path}")
        return Response.status(Response.Status.FORBIDDEN)
            .entity(
                ApiError(
                    "You are not authorized to perform this action.",
                    Response.Status.FORBIDDEN.statusCode
                )
            )
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}

// A missing/invalid token throws io.quarkus.security.UnauthorizedException (401).
// INFO (an unauthenticated request is expected traffic, not an error).
@Provider
class UnauthorizedExceptionMapper : ExceptionMapper<UnauthorizedException> {

    @Context
    lateinit var uriInfo: UriInfo

    override fun toResponse(exception: UnauthorizedException): Response {
        Log.info("Unauthenticated request (401) for ${uriInfo.path}")
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(
                ApiError("Authentication required.", Response.Status.UNAUTHORIZED.statusCode)
            )
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
