package io.pythia.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event

// Thin in-process hook fired after every mutating session operation
// (task-064). Chosen mechanism: a CDI `Event<SessionChangedEvent>`. task-065
// (WebSocket push) subscribes with a `@Observes SessionChangedEvent` observer —
// it plugs in WITHOUT editing this class or EstimationSessionService.

data class SessionChangedEvent(val sessionId: String)

@ApplicationScoped
class SessionEventPublisher(
    private val event: Event<SessionChangedEvent>
) {
    fun published(sessionId: String) {
        event.fire(SessionChangedEvent(sessionId))
    }
}
