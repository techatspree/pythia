package io.github.theestimator.rest

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.emptyString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class UndoEndpointsIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Undo Endpoints Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply { offer = "UNDO-EP-1"; this.project = project }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    private fun asUser(subject: String): RequestSpecification =
        given().header("Authorization", "Dev $subject").contentType(ContentType.JSON)

    private fun createDraft(subject: String = "dev-admin") {
        asUser(subject).post("/api/estimations/$estimationId/versions").then().statusCode(201)
    }

    private fun putNotes(notes: String, subject: String = "dev-admin") {
        asUser(subject).body("""{"notes":"$notes"}""")
            .put("/api/estimations/$estimationId/versions/draft").then().statusCode(200)
    }

    @Test
    fun `undo then redo round-trips the draft state`() {
        createDraft()
        putNotes("v1")

        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/undo")
            .then().statusCode(200).body("notes", equalTo(""))

        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/redo")
            .then().statusCode(200).body("notes", equalTo("v1"))
    }

    @Test
    fun `undo with no draft is 404`() {
        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/undo")
            .then().statusCode(404)
    }

    @Test
    fun `undo with nothing to undo is 404, redo likewise`() {
        createDraft()
        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/undo")
            .then().statusCode(404)
        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/redo")
            .then().statusCode(404)
    }

    @Test
    fun `a second user's change makes the first user's undo a 409 with conflict details`() {
        createDraft()
        putNotes("A", subject = "dev-admin")
        putNotes("B on top", subject = "dev-estimator")

        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/undo")
            .then().statusCode(409)
            .body("blockingUserDisplayName", not(emptyString()))
            .body("blockingSequenceNumber", greaterThan(0))
    }

    @Test
    fun `history returns the entry as UNDONE after an update then undo`() {
        createDraft()
        putNotes("v1")
        asUser("dev-admin").post("/api/estimations/$estimationId/versions/draft/undo").then().statusCode(200)

        asUser("dev-admin").get("/api/estimations/$estimationId/versions/draft/history")
            .then().statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].sequenceNumber", equalTo(1))
            .body("[0].status", equalTo("UNDONE"))
    }
}
