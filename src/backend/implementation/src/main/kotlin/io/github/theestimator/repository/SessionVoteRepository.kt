package io.github.theestimator.repository

import io.github.theestimator.domain.session.SessionPhase
import io.github.theestimator.domain.session.SessionVote
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SessionVoteRepository : PanacheRepositoryBase<SessionVote, UUID> {

    fun findBySession(sessionId: UUID): List<SessionVote> =
        list("session.id", sessionId)

    fun findByItemAndPhase(sessionItemId: UUID, phase: SessionPhase): List<SessionVote> =
        list("sessionItem.id = ?1 AND phase = ?2", sessionItemId, phase)
}
