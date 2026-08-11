package io.pythia.ws

import com.fasterxml.jackson.databind.ObjectMapper
import io.pythia.service.EstimationSessionService
import io.pythia.service.SessionChangedEvent
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import io.quarkus.websockets.next.OpenConnections
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.TransactionPhase
import java.util.UUID

// Push-only realtime fan-out (task-065). Observes task-064's
// SessionChangedEvent and sends the fresh SessionDto to every connection of
// that session. Observing AFTER_SUCCESS means the broadcast reflects the
// committed state and does not do socket I/O inside the mutating transaction.
// PHASE1 vote hiding is uniform (task-064 buildDto), so the SAME payload goes
// to every recipient — no per-connection role scoping.
@ApplicationScoped
class SessionBroadcaster(
    private val sessionService: EstimationSessionService,
    private val connections: OpenConnections,
    private val objectMapper: ObjectMapper
) {

    fun onChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) event: SessionChangedEvent) {
        val sessionId = event.sessionId
        val message = sessionMessage(UUID.fromString(sessionId))
        val targets = connections.listAll().filter { it.pathParam("sessionId") == sessionId }
        Log.debug("Broadcasting session $sessionId to ${targets.size} connection(s)")
        targets.forEach { send(it, message) }
    }

    // Sends the current snapshot to a single connection (used on WS open).
    fun sendSnapshot(connection: WebSocketConnection, sessionId: UUID) {
        send(connection, sessionMessage(sessionId))
    }

    // Application-level heartbeat (task-147). The client cannot distinguish a
    // quiet room from a dead socket on its own — JS can neither send protocol
    // pings nor observe pongs — so this frame is what feeds its watchdog. It
    // carries no state: the client resets its idle timer and drops it.
    //
    // send() blocks (sendTextAndAwait), which is fine here: Quarkus runs
    // @Scheduled methods on a worker thread, so unlike SessionSocket.onOpen —
    // an event-loop callback that needs @Blocking — this does not.
    @Scheduled(every = "{app.session.heartbeat-interval}")
    fun heartbeat() {
        val targets = connections.listAll()
        if (targets.isEmpty()) return
        // DEBUG, never INFO: this fires every 20s for the life of the process.
        Log.debug("Sending heartbeat to ${targets.size} session connection(s)")
        targets.forEach { send(it, HEARTBEAT_MESSAGE) }
    }

    private fun sessionMessage(sessionId: UUID): String =
        objectMapper.writeValueAsString(
            mapOf("type" to "session", "session" to sessionService.getSession(sessionId))
        )

    // A single dead/slow connection must not abort the fan-out to the others,
    // so catch broadly and log rather than propagate.
    @Suppress("TooGenericExceptionCaught")
    private fun send(connection: WebSocketConnection, message: String) {
        try {
            connection.sendTextAndAwait(message)
        } catch (e: Exception) {
            Log.error("Failed to push session message to connection ${connection.id()}", e)
        }
    }

    private companion object {
        private const val HEARTBEAT_MESSAGE = """{"type":"heartbeat"}"""
    }
}
