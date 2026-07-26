package io.github.theestimator.ws

import io.github.theestimator.service.EstimationSessionService
import io.github.theestimator.service.SessionTicketStore
import io.quarkus.logging.Log
import io.quarkus.websockets.next.CloseReason
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.common.annotation.Blocking
import java.util.UUID

// Push-only session channel (task-065). The handshake is authenticated by a
// single-use ticket on the query string (browsers cannot set an Authorization
// header on a WS handshake). Mutations still go through the REST control plane
// (task-064); the server never reads domain messages from the socket.
@WebSocket(path = "/ws/sessions/{sessionId}")
class SessionSocket(
    private val ticketStore: SessionTicketStore,
    private val sessionService: EstimationSessionService,
    private val broadcaster: SessionBroadcaster
) {

    @OnOpen
    @Blocking
    fun onOpen(connection: WebSocketConnection) {
        val sessionIdParam = connection.pathParam("sessionId")
        val sessionId = runCatching { UUID.fromString(sessionIdParam) }.getOrNull()
        val ticket = queryParam(connection, "ticket")
        val subjectId = if (sessionId != null && ticket != null) ticketStore.consume(ticket, sessionId) else null

        if (sessionId == null || subjectId == null || !sessionService.isParticipant(sessionId, subjectId)) {
            Log.warn("Rejecting WS handshake for session $sessionIdParam: invalid or expired ticket")
            connection.sendTextAndAwait("""{"type":"error","message":"Invalid or expired ticket"}""")
            connection.closeAndAwait(CloseReason(POLICY_VIOLATION, "Invalid or expired ticket"))
            return
        }

        Log.info("WS opened for session $sessionId by $subjectId")
        broadcaster.sendSnapshot(connection, sessionId)
    }

    @OnClose
    fun onClose(connection: WebSocketConnection) {
        Log.info("WS closed for session ${connection.pathParam("sessionId")}")
    }

    // websockets-next has no query-param accessor; parse the raw handshake query.
    private fun queryParam(connection: WebSocketConnection, name: String): String? {
        val query = connection.handshakeRequest().query() ?: return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == name }
            ?.get(1)
    }

    private companion object {
        private const val POLICY_VIOLATION = 1008
    }
}
