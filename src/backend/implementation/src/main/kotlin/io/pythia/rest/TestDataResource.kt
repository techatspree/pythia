package io.pythia.rest

import io.pythia.domain.session.EstimationSession
import io.pythia.repository.ProjectRepository
import io.pythia.testdata.SeededProjects
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.logging.Log
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * Resets the e2e fixture data (task-176).
 *
 * `@IfBuildProfile("dev")` is the point, not a formality: the class is not
 * built into a production image AT ALL, which is a stronger guarantee than an
 * authorization check on a destructive endpoint. That is why this exists rather
 * than a `DELETE /api/projects/{id}` on the production surface — the suite
 * creates roughly one project per test and never removes them, and after a few
 * runs the accumulation slows list pages into 30-second interaction timeouts
 * and makes `.first()` locators resolve to the wrong row.
 */
@Path("/api/dev/test-data")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@IfBuildProfile("dev")
@RolesAllowed("ADMIN")
class TestDataResource(
    private val projectRepository: ProjectRepository,
    private val entityManager: EntityManager
) {

    @DELETE
    @Transactional
    fun reset(): Map<String, Long> {
        try {
            // Sessions FIRST. `EstimationSession` holds a plain @ManyToOne to
            // estimation with `nullable = false` and NO cascade from that side,
            // so a session row pointing at an estimation being deleted violates
            // the FK.
            //
            // SELECT then remove(), NOT a bulk `delete from EstimationSession`.
            // A bulk JPQL delete bypasses the persistence context and issues raw
            // SQL, so the session's OWN children (items, participants, votes) are
            // never cascaded and the database rejects the delete:
            //   violates foreign key constraint … on table "session_items"
            // The JPA cascade is an entity-graph operation; it only runs for a
            // managed entity. Loading them keeps this correct if a fifth session
            // table is ever added, and the volumes here are test data.
            val doomedSessions = entityManager
                .createQuery(
                    "select s from EstimationSession s where s.estimation.project.name not in :keep",
                    EstimationSession::class.java
                )
                .setParameter("keep", SeededProjects.ALL)
                .resultList

            // VOTES FIRST, and they are the exception. `EstimationSession` owns
            // its items and participants with `cascade = ALL, orphanRemoval`, so
            // removing the session takes those with it — but votes are NOT one of
            // those collections (the entity's own comment says they are queried
            // via the repository instead), and a vote references an ITEM. So the
            // cascade deletes items out from under the votes and PostgreSQL
            // rejects it:
            //   violates foreign key constraint … on table "session_votes"
            // A vote carries `session_id` as well, so it can be scoped without a
            // join.
            if (doomedSessions.isNotEmpty()) {
                entityManager
                    .createQuery("delete from SessionVote v where v.session.id in :ids")
                    .setParameter("ids", doomedSessions.map { it.id })
                    .executeUpdate()
                entityManager.flush()
            }

            doomedSessions.forEach { entityManager.remove(it) }
            entityManager.flush()
            val deletedSessions = doomedSessions.size.toLong()

            // Then the projects. Estimations, versions, nodes, buckets and
            // schedule dependencies all cascade from here
            // (`cascade = ALL, orphanRemoval = true`).
            val doomed = projectRepository.list("name not in ?1", SeededProjects.ALL)
            val deletedProjects = doomed.size.toLong()
            doomed.forEach { projectRepository.delete(it) }
            projectRepository.flush()

            val remaining = projectRepository.count()
            // `remaining` is not decoration: "deleted 228, 3 remain" makes a
            // mis-scoped keep-list obvious at once, whereas a deleted-count
            // alone looks equally healthy whether the fixture survived or was
            // wiped along with everything else.
            Log.info(
                "e2e reset: deleted $deletedProjects project(s) and $deletedSessions session(s), " +
                    "$remaining project(s) remain"
            )
            return mapOf(
                "deletedProjects" to deletedProjects,
                "deletedSessions" to deletedSessions,
                "remainingProjects" to remaining
            )
        } catch (e: PersistenceException) {
            // Narrow on purpose: the failure mode this endpoint actually has is a
            // constraint violation from deleting in the wrong order, and Hibernate
            // wraps those in PersistenceException. Catching RuntimeException here
            // would also swallow the 403 the security layer throws.
            Log.error("e2e reset failed", e)
            throw e
        }
    }
}
