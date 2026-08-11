package io.pythia.service

import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Short-lived, single-use WebSocket handshake tickets (task-065). Browsers
// cannot set an Authorization header on a WS handshake and the auth is
// provider-specific, so an authenticated participant first POSTs
// /api/sessions/{id}/ws-ticket to mint an opaque token, then opens
// ws://…/ws/sessions/{id}?ticket=…. The token maps to (sessionId, subjectId),
// expires after TTL, and is consumed on first use.
@ApplicationScoped
class SessionTicketStore {

    private data class Entry(val sessionId: UUID, val subjectId: String, val expiresAt: Instant)

    private val tickets = ConcurrentHashMap<String, Entry>()
    private val ttl = Duration.ofSeconds(TTL_SECONDS)

    fun issue(sessionId: UUID, subjectId: String): String {
        val ticket = UUID.randomUUID().toString().replace("-", "")
        tickets[ticket] = Entry(sessionId, subjectId, Instant.now().plus(ttl))
        return ticket
    }

    // Single use: removes the entry regardless of validity. Returns the
    // subjectId only when the ticket matches the session and is unexpired.
    fun consume(ticket: String, sessionId: UUID): String? {
        val entry = tickets.remove(ticket) ?: return null
        if (entry.sessionId != sessionId) return null
        if (Instant.now().isAfter(entry.expiresAt)) return null
        return entry.subjectId
    }

    private companion object {
        private const val TTL_SECONDS = 30L
    }
}
