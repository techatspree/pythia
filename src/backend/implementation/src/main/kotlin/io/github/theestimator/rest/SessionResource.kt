package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.rest.dto.CreateSessionRequest
import io.github.theestimator.rest.dto.NotesRequest
import io.github.theestimator.rest.dto.SessionDto
import io.github.theestimator.rest.dto.VoteRequest
import io.github.theestimator.rest.dto.WsTicketDto
import io.github.theestimator.service.CurrentUserService
import io.github.theestimator.service.EstimationSessionService
import io.github.theestimator.service.SessionTicketStore
import io.quarkus.logging.Log
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

// Collaborative estimation session control plane (task-064). Every endpoint
// requires ESTIMATOR; moderator-only actions additionally check ownership in
// the service (→ 403), and vote/phase guards return 409. Aggregation is
// delegated to the domain VoteAggregation. Real-time push is task-065.
@Path("/api/sessions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ESTIMATOR")
@Tag(name = "Estimation sessions", description = "Collaborative two-phase Wideband-Delphi estimation")
@Suppress("TooManyFunctions") // one endpoint method per session lifecycle action
class SessionResource(
    private val sessionService: EstimationSessionService,
    private val currentUserProvider: CurrentUserProvider,
    private val currentUserService: CurrentUserService,
    private val ticketStore: SessionTicketStore
) {

    @POST
    @Path("/{id}/ws-ticket")
    @Operation(summary = "Issue a short-lived single-use WebSocket handshake ticket for the current participant")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = WsTicketDto::class))])
    @APIResponse(responseCode = "403", description = "Not a participant of the session")
    @APIResponse(responseCode = "404", description = "Session not found")
    fun wsTicket(@PathParam("id") id: UUID): Response {
        val subjectId = currentUserProvider.get().subjectId
        sessionService.assertParticipant(id, subjectId)
        val ticket = ticketStore.issue(id, subjectId)
        Log.info("Issued WS ticket for session $id to $subjectId")
        return Response.ok(WsTicketDto(ticket)).build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a session (the current user becomes the moderator)")
    @APIResponse(
        responseCode = "201",
        description = "Session created",
        content = [Content(schema = Schema(implementation = SessionDto::class))]
    )
    @APIResponse(responseCode = "400", description = "A logicalId is not a draft leaf, or the estimation has no draft")
    @APIResponse(responseCode = "403", description = "Not an ESTIMATOR")
    fun create(body: CreateSessionRequest): Response {
        val user = currentUserProvider.get()
        val dto = sessionService.createSession(
            body.estimationId, body.title, body.itemLogicalIds, user.subjectId, user.displayName
        )
        return Response.status(Response.Status.CREATED).entity(dto).build()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Read a session (blind items expose only a vote count)")
    @APIResponse(
        responseCode = "200",
        content = [Content(schema = Schema(implementation = SessionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Session not found")
    fun get(@PathParam("id") id: UUID): Response =
        Response.ok(sessionService.getSession(id)).build()

    @GET
    @Operation(
        summary = "List sessions — of one estimation when estimationId is given, " +
            "otherwise all joinable (CREATED/RUNNING) sessions"
    )
    @APIResponse(
        responseCode = "200",
        content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = SessionDto::class))]
    )
    fun list(
        @Parameter(required = false, description = "Filter to one estimation; omit for all joinable sessions")
        @QueryParam("estimationId") estimationId: UUID?
    ): Response {
        val sessions = if (estimationId != null) {
            sessionService.listSessions(estimationId)
        } else {
            sessionService.listJoinableSessions()
        }
        return Response.ok(sessions).build()
    }

    @POST
    @Path("/{id}/join")
    @Operation(summary = "Join the current user as an ESTIMATOR participant (idempotent)")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "404", description = "Session not found")
    fun join(@PathParam("id") id: UUID): Response {
        val user = currentUserProvider.get()
        return Response.ok(sessionService.join(id, user.subjectId, user.displayName)).build()
    }

    @POST
    @Path("/{id}/start")
    @Operation(summary = "Moderator: start the session (CREATED → RUNNING)")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "403", description = "Not the moderator")
    @APIResponse(responseCode = "409", description = "Session is not in CREATED")
    fun start(@PathParam("id") id: UUID): Response =
        Response.ok(sessionService.start(id, currentUserProvider.get().subjectId)).build()

    @PUT
    @Path("/{id}/items/current/notes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Moderator: set the current item's discussion notes")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "403", description = "Not the moderator")
    fun notes(@PathParam("id") id: UUID, body: NotesRequest): Response =
        Response.ok(sessionService.updateCurrentNotes(id, currentUserProvider.get().subjectId, body.notes)).build()

    @POST
    @Path("/{id}/items/current/phase2")
    @Operation(summary = "Moderator: reveal — advance the current item to PHASE2")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "403", description = "Not the moderator")
    @APIResponse(responseCode = "409", description = "Current item is not in PHASE1")
    fun reveal(@PathParam("id") id: UUID): Response =
        Response.ok(sessionService.revealPhase2(id, currentUserProvider.get().subjectId)).build()

    @POST
    @Path("/{id}/items/current/finalize")
    @Operation(summary = "Moderator: finalize the current item (writes back to the draft) and advance")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "403", description = "Not the moderator")
    @APIResponse(responseCode = "409", description = "Current item is not in PHASE2, or has no votes")
    fun finalize(@PathParam("id") id: UUID): Response {
        val user = currentUserService.ensureUser(currentUserProvider.get())
        return Response.ok(sessionService.finalizeCurrent(id, user)).build()
    }

    @POST
    @Path("/{id}/votes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Estimator: upsert a vote for the current item + current phase")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "409", description = "Not a participant, session not RUNNING, or no current item")
    fun vote(@PathParam("id") id: UUID, body: VoteRequest): Response {
        val dto = sessionService.submitVote(
            id, currentUserProvider.get().subjectId, body.minEffort, body.expectedEffort, body.maxEffort
        )
        return Response.ok(dto).build()
    }

    @POST
    @Path("/{id}/agree")
    @Operation(summary = "Estimator: mark agreement on the current item")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "409", description = "Not a participant")
    fun agree(@PathParam("id") id: UUID): Response =
        Response.ok(sessionService.agree(id, currentUserProvider.get().subjectId)).build()

    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "Moderator: cancel the session")
    @APIResponse(responseCode = "200", content = [Content(schema = Schema(implementation = SessionDto::class))])
    @APIResponse(responseCode = "403", description = "Not the moderator")
    fun cancel(@PathParam("id") id: UUID): Response =
        Response.ok(sessionService.cancel(id, currentUserProvider.get().subjectId)).build()
}
