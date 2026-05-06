package io.github.theestimator.rest

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class EstimationVersionResourceIT {

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
            offer = "TEST-001"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @Test
    fun `create draft returns 201 with version data`() {
        given()
            .`when`().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(1))
            .body("isDraft", equalTo(true))
            .body("totalEffort", notNullValue())
    }

    @Test
    fun `create duplicate draft returns 409`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .`when`().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(409)
    }

    @Test
    fun `get draft returns on-the-fly calculations`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("isDraft", equalTo(true))
            .body("versionNumber", equalTo(1))
            .body("parameters", notNullValue())
    }

    @Test
    fun `get draft when none exists returns 404`() {
        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(404)
    }

    @Test
    fun `update draft changes parameters`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "notes": "Updated notes",
                    "parameters": [
                        {"name": "Tagessatz", "value": 1000.0},
                        {"name": "Standardabweichungsfaktor", "value": 2.5}
                    ]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("notes", equalTo("Updated notes"))
            .body("parameters.size()", equalTo(2))
            .body("parameters.find { it.name == 'Tagessatz' }.value", equalTo(1000.0f))
    }

    @Test
    fun `submit draft creates submitted version and removes draft`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"parameters": [{"name": "Tagessatz", "value": 900.0}, {"name": "Standardabweichungsfaktor", "value": 2.0}, {"name": "Vertriebszuschlag", "value": 0.12}]}""")
            .put("/api/estimations/$estimationId/versions/draft")

        given()
            .`when`().post("/api/estimations/$estimationId/versions/draft/submit")
            .then()
            .statusCode(200)
            .body("isDraft", equalTo(false))
            .body("versionNumber", equalTo(1))
            .body("submittedAt", notNullValue())
            .body("totalEffort", notNullValue())

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(404)
    }

    @Test
    fun `get submitted version by number`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("isDraft", equalTo(false))
            .body("versionNumber", equalTo(1))
    }

    @Test
    fun `list versions returns draft first then submitted`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].isDraft", equalTo(true))
            .body("[0].versionNumber", equalTo(2))
            .body("[1].isDraft", equalTo(false))
            .body("[1].versionNumber", equalTo(1))
    }

    @Test
    fun `delete draft removes it`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .`when`().delete("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(204)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(404)
    }

    @Test
    fun `new draft after submit clones from previous version`() {
        given().post("/api/estimations/$estimationId/versions")
        given()
            .contentType(ContentType.JSON)
            .body("""{"parameters": [{"name": "Tagessatz", "value": 950.0}]}""")
            .put("/api/estimations/$estimationId/versions/draft")
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(2))
            .body("parameters.find { it.name == 'Tagessatz' }.value", equalTo(950.0f))
    }

    @Test
    fun `nonexistent estimation returns 404`() {
        val fakeId = UUID.randomUUID()

        given()
            .`when`().get("/api/estimations/$fakeId/versions")
            .then()
            .statusCode(404)
    }
}
