package io.github.theestimator.rest

import io.github.theestimator.domain.*
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationVersionRepository
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.service.EstimationCalculator
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

@QuarkusTest
class EstimationVersionResourceIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var versionRepository: EstimationVersionRepository

    @Inject
    lateinit var calculator: EstimationCalculator

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "REST Test Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "REST Test Offer"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @Test
    fun `list versions returns empty list for new estimation`() {
        given()
            .`when`().get("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(200)
            .body("$", hasSize<Any>(0))
    }

    @Test
    fun `create version returns 201 with version 1`() {
        given()
            .`when`().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(1))
            .body("status", equalTo("DRAFT"))
    }

    @Test
    fun `create second version increments version number`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(2))
    }

    @Test
    fun `get version returns full details`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("versionNumber", equalTo(1))
            .body("status", equalTo("DRAFT"))
            .body("parameters", notNullValue())
            .body("itemGroups", notNullValue())
    }

    @Test
    fun `get nonexistent version returns 404`() {
        given()
            .`when`().get("/api/estimations/$estimationId/versions/99")
            .then()
            .statusCode(404)
    }

    @Test
    fun `get version for nonexistent estimation returns 404`() {
        given()
            .`when`().get("/api/estimations/${UUID.randomUUID()}/versions/1")
            .then()
            .statusCode(404)
    }

    @Test
    fun `submit version transitions to SUBMITTED`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().post("/api/estimations/$estimationId/versions/1/submit")
            .then()
            .statusCode(200)
            .body("status", equalTo("SUBMITTED"))
    }

    @Test
    fun `submit already submitted version returns 409`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/1/submit")

        given()
            .`when`().post("/api/estimations/$estimationId/versions/1/submit")
            .then()
            .statusCode(409)
            .body("error", containsString("SUBMITTED"))
    }

    @Test
    fun `update DRAFT version succeeds`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .contentType(ContentType.JSON)
            .body("""{"notes": "Updated notes"}""")
            .`when`().put("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("notes", equalTo("Updated notes"))
    }

    @Test
    fun `update SUBMITTED version returns 409`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/1/submit")

        given()
            .contentType(ContentType.JSON)
            .body("""{"notes": "Should fail"}""")
            .`when`().put("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(409)
            .body("error", containsString("SUBMITTED"))
    }

    @Test
    fun `delete DRAFT version returns 204`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().delete("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(204)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(404)
    }

    @Test
    fun `delete SUBMITTED version returns 409`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/1/submit")

        given()
            .`when`().delete("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(409)
    }

    @Test
    fun `list versions returns created versions`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(200)
            .body("$", hasSize<Any>(2))
            .body("[0].versionNumber", notNullValue())
    }

    @Test
    fun `import from Excel creates version with items`() {
        val file = resolveReferenceSpreadsheet() ?: return
        given()
            .multiPart("file", file)
            .`when`().post("/api/estimations/$estimationId/versions/import")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(1))
            .body("status", equalTo("DRAFT"))
            .body("totalEffort", greaterThan(0.0f))
            .body("itemGroups", not(empty<Any>()))
    }

    @Test
    fun `export version returns xlsx`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .header("Content-Disposition", containsString("estimation_v1.xlsx"))
    }

    @Test
    fun `update parameters triggers recalculation`() {
        // Import a version with items first
        val file = resolveReferenceSpreadsheet() ?: return
        given().multiPart("file", file).post("/api/estimations/$estimationId/versions/import")

        given()
            .contentType(ContentType.JSON)
            .body("""{"parameters": [
                {"name": "Standardabweichungsfaktor", "value": 3.0},
                {"name": "Tagessatz", "value": 1000.0},
                {"name": "Vertriebszuschlag", "value": 0.15}
            ]}""")
            .`when`().put("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("totalEffort", greaterThan(0.0f))
    }

    private fun resolveReferenceSpreadsheet(): File? {
        val file = File("../../planning/inputdata/reference-spreadsheet.xlsx")
        if (file.exists()) return file
        val absFile = File(System.getProperty("user.dir") + "/../../../planning/inputdata/reference-spreadsheet.xlsx")
        if (absFile.exists()) return absFile
        return null
    }
}
