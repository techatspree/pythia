package io.pythia.rest

import io.pythia.auth.DevAdminAuth
import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.repository.EstimationRepository
import io.pythia.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

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
            .body("method", equalTo("THREE_POINT_PERT"))
            .body("methodDescription", not(emptyOrNullString()))
            .body("methodDescription", containsString("PERT"))
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
