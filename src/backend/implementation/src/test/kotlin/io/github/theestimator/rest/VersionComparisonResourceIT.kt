package io.github.theestimator.rest

import io.github.theestimator.domain.*
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationVersionRepository
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
class VersionComparisonResourceIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var versionRepository: EstimationVersionRepository

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Comparison Test Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "Comparison Test Offer"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @Test
    fun `compare identical versions returns empty diffs`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("versionA", equalTo(1))
            .body("versionB", equalTo(2))
            .body("addedItems", hasSize<Any>(0))
            .body("removedItems", hasSize<Any>(0))
            .body("modifiedItems", hasSize<Any>(0))
            .body("addedGroups", hasSize<Any>(0))
            .body("removedGroups", hasSize<Any>(0))
            .body("parameterChanges", hasSize<Any>(0))
    }

    @Test
    fun `compare with modified parameters detects changes`() {
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .contentType(ContentType.JSON)
            .body("""{"parameters": [
                {"name": "Tagessatz", "value": 1000.0},
                {"name": "Standardabweichungsfaktor", "value": 3.0}
            ]}""")
            .put("/api/estimations/$estimationId/versions/2")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("parameterChanges", not(empty<Any>()))
    }

    @Test
    fun `compare nonexistent version returns 404`() {
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/99")
            .then()
            .statusCode(404)
    }

    @Test
    fun `compare with imported versions detects item changes`() {
        val file = resolveReferenceSpreadsheet() ?: return
        given().multiPart("file", file).post("/api/estimations/$estimationId/versions/import")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("versionA", equalTo(1))
            .body("versionB", equalTo(2))
            .body("addedItems", hasSize<Any>(0))
            .body("removedItems", hasSize<Any>(0))
            .body("modifiedItems", hasSize<Any>(0))
    }

    private fun resolveReferenceSpreadsheet(): java.io.File? {
        val file = java.io.File("../../planning/inputdata/reference-spreadsheet.xlsx")
        if (file.exists()) return file
        val absFile = java.io.File(System.getProperty("user.dir") + "/../../../planning/inputdata/reference-spreadsheet.xlsx")
        if (absFile.exists()) return absFile
        return null
    }
}
