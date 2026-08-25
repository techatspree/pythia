package io.pythia.rest

import io.pythia.auth.DevAdminAuth
import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.domain.draft.DraftScheduleDependency
import io.pythia.repository.DraftEstimationVersionRepository
import io.pythia.repository.EstimationRepository
import io.pythia.repository.ProjectRepository
import io.pythia.service.DraftUpdateApplier
import io.pythia.service.toUpdateDto
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

/**
 * The schedule inputs task-155 computes from but could not store: the team size
 * and the finish-to-start edges between root nodes.
 *
 * Each test here pins one of the three paths that silently loses them.
 */
@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class ProjectScheduleInputsIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var draftRepository: DraftEstimationVersionRepository

    @Inject
    lateinit var applier: DraftUpdateApplier

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Schedule Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "SCHED-001"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    private fun createDraft() =
        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

    private fun putSchedule(teamFte: Double) = given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
                "teamFte": $teamFte,
                "dependencies": [
                    { "fromLogicalId": "a", "toLogicalId": "b" },
                    { "fromLogicalId": "b", "toLogicalId": "c" }
                ]
            }
            """.trimIndent()
        )
        .`when`().put("/api/estimations/$estimationId/versions/draft")

    @Test
    fun `a draft round-trips its team size and dependency edges`() {
        createDraft()

        putSchedule(3.0).then()
            .statusCode(200)
            .body("teamFte", equalTo(3.0f))
            .body("dependencies", hasSize<Any>(2))
            .body("dependencies.fromLogicalId", contains("a", "b"))

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("teamFte", equalTo(3.0f))
            .body("dependencies", hasSize<Any>(2))
    }

    @Test
    fun `a default draft is schedulable — teamFte is 1, never 0`() {
        // 0 is the one team size task-155 rejects, so the column default has to
        // leave every version computable without the editor being opened.
        createDraft()

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("teamFte", equalTo(1.0f))
            .body("dependencies", hasSize<Any>(0))
    }

    @Test
    fun `omitting the schedule fields does not wipe a saved schedule`() {
        createDraft()
        putSchedule(4.0).then().statusCode(200)

        // An older client that knows nothing about the schedule.
        given()
            .contentType(ContentType.JSON)
            .body("""{ "notes": "unrelated edit" }""")
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("teamFte", equalTo(4.0f))
            .body("dependencies", hasSize<Any>(2))
    }

    @Test
    fun `submitting carries the schedule into the snapshot`() {
        // The snapshotDraft path. Missing it renders an empty Gantt on a
        // submitted version where the draft rendered a full one.
        createDraft()
        putSchedule(2.5).then().statusCode(200)

        given().post("/api/estimations/$estimationId/versions/draft/submit")
            .then().statusCode(200)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("teamFte", equalTo(2.5f))
            .body("dependencies", hasSize<Any>(2))
            .body("dependencies.toLogicalId", contains("b", "c"))
    }

    @Test
    fun `a draft cloned from a submitted version inherits its schedule`() {
        // The third copy path: cloneFromSubmitted. Without it, continuing an
        // estimate silently starts over with no dependencies.
        createDraft()
        putSchedule(2.0).then().statusCode(200)
        given().post("/api/estimations/$estimationId/versions/draft/submit")
            .then().statusCode(200)

        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("teamFte", equalTo(2.0f))
            .body("dependencies", hasSize<Any>(2))
    }

    @Test
    fun `the undo capture round-trips the schedule`() {
        // toUpdateDto() is the inverse of DraftUpdateApplier, so
        // apply(draft, draft.toUpdateDto()) must be an identity on state.
        // Without the capture half, an undo silently wipes the schedule.
        createDraft()
        putSchedule(5.0).then().statusCode(200)

        QuarkusTransaction.requiringNew().run {
            val draft = draftRepository.findByEstimationId(estimationId)!!
            val captured = draft.toUpdateDto()
            assertEquals(5.0, captured.teamFte)
            assertEquals(2, captured.dependencies?.size)

            applier.apply(draft, captured)

            assertEquals(5.0, draft.teamFte)
            assertEquals(2, draft.scheduleDependencies.size)
            assertEquals("a", draft.scheduleDependencies[0].fromLogicalId)
            assertEquals("c", draft.scheduleDependencies[1].toLogicalId)
        }
    }

    @Test
    fun `a duplicate dependency edge is rejected`() {
        // Passes ONLY if the UNIQUE constraint is on the ENTITY: under %test
        // Hibernate builds the schema and the migration never runs.
        createDraft()

        val failed = runCatching {
            QuarkusTransaction.requiringNew().run {
                val draft = draftRepository.findByEstimationId(estimationId)!!
                repeat(2) {
                    draft.scheduleDependencies.add(DraftScheduleDependency().apply {
                        fromLogicalId = "x"
                        toLogicalId = "y"
                        version = draft
                    })
                }
                draftRepository.flush()
            }
        }.isFailure

        assertTrue(failed, "the UNIQUE constraint must reject a duplicate (version, from, to)")
    }
}
