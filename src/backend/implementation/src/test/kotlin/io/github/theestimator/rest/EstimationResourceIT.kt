package io.github.theestimator.rest

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.auth.DevAdminAuth
import io.github.theestimator.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.extension.ExtendWith
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class EstimationResourceIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Test Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "EST-IT-001"
            description = "Integration test estimation"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @Test
    fun `get estimation returns detail with empty versions`() {
        given()
            .`when`().get("/api/estimations/$estimationId")
            .then()
            .statusCode(200)
            .body("id", equalTo(estimationId.toString()))
            .body("offer", equalTo("EST-IT-001"))
            .body("description", equalTo("Integration test estimation"))
            .body("hasDraft", equalTo(false))
            .body("versions.size()", equalTo(0))
    }

    @Test
    fun `get estimation includes draft in versions list`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .`when`().get("/api/estimations/$estimationId")
            .then()
            .statusCode(200)
            .body("hasDraft", equalTo(true))
            .body("versions.size()", equalTo(1))
            .body("versions[0].isDraft", equalTo(true))
            .body("versions[0].versionNumber", equalTo(1))
    }

    @Test
    fun `get estimation shows draft and submitted versions`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId")
            .then()
            .statusCode(200)
            .body("hasDraft", equalTo(true))
            .body("versions.size()", equalTo(2))
            .body("versions[0].isDraft", equalTo(true))
            .body("versions[0].versionNumber", equalTo(2))
            .body("versions[1].isDraft", equalTo(false))
            .body("versions[1].versionNumber", equalTo(1))
    }

    @Test
    fun `submit version flow - draft becomes submitted`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .`when`().get("/api/estimations/$estimationId")
            .then()
            .body("hasDraft", equalTo(true))
            .body("versions[0].isDraft", equalTo(true))

        given().post("/api/estimations/$estimationId/versions/draft/submit")
            .then().statusCode(200)

        given()
            .`when`().get("/api/estimations/$estimationId")
            .then()
            .statusCode(200)
            .body("hasDraft", equalTo(false))
            .body("latestVersionNumber", equalTo(1))
            .body("versions.size()", equalTo(1))
            .body("versions[0].isDraft", equalTo(false))
            .body("versions[0].versionNumber", equalTo(1))
            .body("versions[0].totalEffort", notNullValue())
    }

    @Test
    fun `nonexistent estimation returns 404`() {
        val fakeId = UUID.randomUUID()
        given()
            .`when`().get("/api/estimations/$fakeId")
            .then()
            .statusCode(404)
    }
}
