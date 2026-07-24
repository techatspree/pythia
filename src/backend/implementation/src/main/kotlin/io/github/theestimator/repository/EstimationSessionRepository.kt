package io.github.theestimator.repository

import io.github.theestimator.domain.session.EstimationSession
import io.github.theestimator.domain.session.SessionParticipant
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class EstimationSessionRepository : PanacheRepositoryBase<EstimationSession, UUID> {

    fun findByEstimationId(estimationId: UUID): List<EstimationSession> =
        list("estimation.id", estimationId)

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
