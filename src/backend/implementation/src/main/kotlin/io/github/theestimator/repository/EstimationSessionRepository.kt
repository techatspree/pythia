package io.github.theestimator.repository

import io.github.theestimator.domain.session.EstimationSession
import io.github.theestimator.domain.session.SessionParticipant
import io.github.theestimator.domain.session.SessionStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class EstimationSessionRepository : PanacheRepositoryBase<EstimationSession, UUID> {

    fun findByEstimationId(estimationId: UUID): List<EstimationSession> =
        list("estimation.id", estimationId)

    // Sessions a user can still join: CREATED, RUNNING or SUSPENDED, across every
    // estimation. A suspended session must stay listed — this list is how a
    // parked room is found again and resumed. The terminal states (FINALIZED,
    // ENDED_EARLY, CANCELLED) are excluded.
    fun findJoinable(): List<EstimationSession> =
        list(
            "status in ?1",
            listOf(SessionStatus.CREATED, SessionStatus.RUNNING, SessionStatus.SUSPENDED)
        )

    fun findParticipant(sessionId: UUID, subjectId: String): SessionParticipant? =
        getEntityManager()
            .createQuery(
                "SELECT p FROM SessionParticipant p WHERE p.session.id = :sessionId AND p.subjectId = :subjectId",
                SessionParticipant::class.java
            )
            .setParameter("sessionId", sessionId)
            .setParameter("subjectId", subjectId)
            .resultList
            .firstOrNull()
}
