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
        buildRealisticDraft(estimationId)

        given()
            .`when`().post("/api/estimations/$estimationId/versions/draft/submit")
            .then()
            .statusCode(200)
            .body("isDraft", equalTo(false))
            .body("versionNumber", equalTo(1))
            .body("submittedAt", notNullValue())
            .body("totalEffort", notNullValue())
            .body("itemGroups[0].items.size()", equalTo(2))
            .body("itemGroups[0].items[0].offerPT", greaterThan(0.0f))

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
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(2))
            .body("parameters.find { it.name == 'Tagessatz' }.value", equalTo(900.0f))
            .body("effortDrivers.size()", equalTo(1))
            .body("effortDrivers[0].description", equalTo("QA"))
            .body("itemGroups[0].items.size()", equalTo(2))
    }

    @Test
    fun `update draft with item groups persists items`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "itemGroups": [{
                        "title": "Backend",
                        "items": [
                            {"description": "Task A", "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0},
                            {"description": "Task B", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0, "assumptions": "Needs design"}
                        ]
                    }]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups.size()", equalTo(1))
            .body("itemGroups[0].title", equalTo("Backend"))
            .body("itemGroups[0].items.size()", equalTo(2))
            .body("itemGroups[0].items[0].description", equalTo("Task A"))
            .body("itemGroups[0].items[0].minEffort", equalTo(1.0f))
            .body("itemGroups[0].items[0].expectedEffort", equalTo(2.0f))
            .body("itemGroups[0].items[0].maxEffort", equalTo(3.0f))
            .body("itemGroups[0].items[0].mean", equalTo(2.0f))
            .body("itemGroups[0].items[1].assumptions", equalTo("Needs design"))
    }

    @Test
    fun `update draft item groups replaces previous groups`() {
        given().post("/api/estimations/$estimationId/versions")
        given()
            .contentType(ContentType.JSON)
            .body("""{"itemGroups": [{"title": "First", "items": [{"description": "Old"}]}]}""")
            .put("/api/estimations/$estimationId/versions/draft")

        given()
            .contentType(ContentType.JSON)
            .body("""{"itemGroups": [{"title": "Second", "items": [{"description": "New", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}]}]}""")
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups.size()", equalTo(1))
            .body("itemGroups[0].title", equalTo("Second"))
            .body("itemGroups[0].items[0].description", equalTo("New"))
            .body("totalEffort", greaterThan(0.0f))
    }

    @Test
    fun `effort drivers increase offerPT`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        // Standardabweichungsfaktor=0 eliminates risk surcharge so offerPT == mean exactly
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [{"name": "Standardabweichungsfaktor", "value": 0.0}],
                    "itemGroups": [{"title": "G", "items": [{"description": "T", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].offerPT", equalTo(4.0f))

        // driver factor=0.5: driverSurcharge = mean*0.5, offerPT = mean + 0 + mean*0.5 = 1.5*mean = 6.0
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [{"name": "Standardabweichungsfaktor", "value": 0.0}],
                    "effortDrivers": [{"description": "QA", "factor": 0.5}],
                    "itemGroups": [{"title": "G", "items": [{"description": "T", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].offerPT", equalTo(6.0f))
            .body("totalEffort", equalTo(6.0f))
    }

    @Test
    fun `Tagessatz affects cost but not offerPT`() {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [
                        {"name": "Tagessatz", "value": 800.0},
                        {"name": "Standardabweichungsfaktor", "value": 0.0}
                    ],
                    "itemGroups": [{"title": "G", "items": [{"description": "T", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].offerPT", equalTo(4.0f))
            .body("itemGroups[0].items[0].cost", equalTo(3200.0f))

        // Doubling Tagessatz doubles cost but leaves offerPT unchanged
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [
                        {"name": "Tagessatz", "value": 1600.0},
                        {"name": "Standardabweichungsfaktor", "value": 0.0}
                    ],
                    "itemGroups": [{"title": "G", "items": [{"description": "T", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].offerPT", equalTo(4.0f))
            .body("itemGroups[0].items[0].cost", equalTo(6400.0f))
            .body("itemGroups[0].items[0].offerPrice", equalTo(7040.0f))
    }

    @Test
    fun `nonexistent estimation returns 404`() {
        val fakeId = UUID.randomUUID()

        given()
            .`when`().get("/api/estimations/$fakeId/versions")
            .then()
            .statusCode(404)
    }

    @Test
    fun `phase assignment per item is saved and cloned`() {
        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

        // PUT with phases and item carrying phaseAbbreviation
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "phases": [{"name": "Analysis", "abbreviation": "AN", "durationWeeks": 2.0}],
                    "parameters": [{"name": "Standardabweichungsfaktor", "value": 0.0}],
                    "itemGroups": [{"title": "G", "items": [
                        {"description": "T", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0, "phaseAbbreviation": "AN"}
                    ]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("phases.size()", equalTo(1))
            .body("itemGroups[0].items[0].phaseAbbreviation", equalTo("AN"))
            .body("itemGroups[0]", not(hasKey("phaseAbbreviation")))

        // Submit and verify phaseAbbreviation on submitted snapshot
        given().post("/api/estimations/$estimationId/versions/draft/submit").then().statusCode(200)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].phaseAbbreviation", equalTo("AN"))
            .body("itemGroups[0]", not(hasKey("phaseAbbreviation")))

        // Clone into new draft — phaseAbbreviation must be preserved
        given().post("/api/estimations/$estimationId/versions")
            .then()
            .statusCode(201)
            .body("versionNumber", equalTo(2))
            .body("itemGroups[0].items[0].phaseAbbreviation", equalTo("AN"))
    }

    @Test
    fun `time-relative item offerPT scales with phase duration`() {
        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "phases": [{"name": "Analysis", "abbreviation": "AN", "durationWeeks": 4.0}],
                    "parameters": [{"name": "Standardabweichungsfaktor", "value": 0.0}],
                    "itemGroups": [{"title": "G", "items": [{
                        "description": "T",
                        "type": "TIME_RELATIVE",
                        "unit": "h/Woche",
                        "minEffort": 1.0,
                        "expectedEffort": 2.0,
                        "maxEffort": 3.0,
                        "phaseAbbreviation": "AN"
                    }]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            // PERT(1,2,3) = 2.0, * 4 weeks = 8.0
            .body("itemGroups[0].items[0].offerPT", equalTo(8.0f))
            .body("itemGroups[0].items[0].unit", equalTo("h/Woche"))
            .body("itemGroups[0].items[0].type", equalTo("TIME_RELATIVE"))
    }

    @Test
    fun `time-relative item without phase has offerPT zero`() {
        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [{"name": "Standardabweichungsfaktor", "value": 0.0}],
                    "itemGroups": [{"title": "G", "items": [{
                        "description": "T",
                        "type": "TIME_RELATIVE",
                        "minEffort": 1.0,
                        "expectedEffort": 2.0,
                        "maxEffort": 3.0
                    }]}]
                }
            """.trimIndent())
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
            .body("itemGroups[0].items[0].offerPT", equalTo(0.0f))
    }

    @Test
    fun `compare endpoint returns 404 for non-existent version`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/99")
            .then()
            .statusCode(404)
    }

    @Test
    fun `comparing identical versions returns empty diff`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("versionA", equalTo(1))
            .body("versionB", equalTo(2))
            .body("addedItems.size()", equalTo(0))
            .body("removedItems.size()", equalTo(0))
            .body("modifiedItems.size()", equalTo(0))
            .body("parameterChanges.size()", equalTo(0))
    }

    @Test
    fun `replaced items appear as removed from v1 and added in v2`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"itemGroups": [{"title": "Development", "items": [
                    {"description": "New Feature X", "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0}
                ]}]}
            """.trimIndent())
            .put("/api/estimations/$estimationId/versions/draft")
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("removedItems.size()", equalTo(2))
            .body("addedItems.size()", equalTo(1))
            .body("addedItems[0].description", equalTo("New Feature X"))
            .body("modifiedItems.size()", equalTo(0))
    }

    @Test
    fun `changed effort values appear in modifiedItems with correct changedFields`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")

        val jp = given().get("/api/estimations/$estimationId/versions/draft")
            .then().extract().jsonPath()
        val groupId = jp.getString("itemGroups[0].logicalId")
        val idA     = jp.getString("itemGroups[0].items[0].logicalId")
        val idB     = jp.getString("itemGroups[0].items[1].logicalId")

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"itemGroups": [{"logicalId": "$groupId", "title": "Development", "items": [
                    {"logicalId": "$idA", "description": "Feature A",
                     "minEffort": 3.0, "expectedEffort": 6.0, "maxEffort": 9.0},
                    {"logicalId": "$idB", "description": "Feature B",
                     "minEffort": 1.0, "expectedEffort": 3.0, "maxEffort": 5.0}
                ]}]}
            """.trimIndent())
            .put("/api/estimations/$estimationId/versions/draft")
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/2")
            .then()
            .statusCode(200)
            .body("modifiedItems.size()", equalTo(1))
            .body("modifiedItems[0].description", equalTo("Feature A"))
            .body("modifiedItems[0].changedFields",
                  containsInAnyOrder("minEffort", "expectedEffort", "maxEffort"))
            .body("modifiedItems[0].before.minEffort", equalTo(2.0f))
            .body("modifiedItems[0].after.minEffort",  equalTo(3.0f))
            .body("addedItems.size()",   equalTo(0))
            .body("removedItems.size()", equalTo(0))
    }

    @Test
    fun `submitted version can be compared against the draft`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")
        given().post("/api/estimations/$estimationId/versions")

        val jp = given().get("/api/estimations/$estimationId/versions/draft")
            .then().extract().jsonPath()
        val groupId = jp.getString("itemGroups[0].logicalId")
        val idA     = jp.getString("itemGroups[0].items[0].logicalId")
        val idB     = jp.getString("itemGroups[0].items[1].logicalId")

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"itemGroups": [{"logicalId": "$groupId", "title": "Development", "items": [
                    {"logicalId": "$idA", "description": "Feature A",
                     "minEffort": 3.0, "expectedEffort": 6.0, "maxEffort": 9.0},
                    {"logicalId": "$idB", "description": "Feature B",
                     "minEffort": 1.0, "expectedEffort": 3.0, "maxEffort": 5.0}
                ]}]}
            """.trimIndent())
            .put("/api/estimations/$estimationId/versions/draft")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/draft")
            .then()
            .statusCode(200)
            .body("modifiedItems.size()", equalTo(1))
            .body("modifiedItems[0].description", equalTo("Feature A"))

        // selecting the draft for comparison must not have submitted it
        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
    }

    @Test
    fun `xlsx export returns a valid spreadsheet`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        val bytes = given()
            .`when`().get("/api/estimations/$estimationId/versions/1/export?format=xlsx")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .header("Content-Disposition", containsString("attachment"))
            .extract().asByteArray()
        // .xlsx is a ZIP — magic bytes 'P','K'
        assert(bytes.size > 2 && bytes[0].toInt() == 0x50 && bytes[1].toInt() == 0x4B)
    }

    @Test
    fun `csv export total matches the version total effort`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        val totalEffort = given().get("/api/estimations/$estimationId/versions/1")
            .then().extract().jsonPath().getDouble("totalEffort")

        val csv = given()
            .`when`().get("/api/estimations/$estimationId/versions/1/export?format=csv")
            .then()
            .statusCode(200)
            .contentType("text/csv")
            .extract().asString()

        val lastCell = csv.trim().lines().last().split(",").last().toDouble()
        assert(Math.abs(lastCell - totalEffort) < 0.001)
    }

    @Test
    fun `export defaults to xlsx when no format given`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    @Test
    fun `export rejects an unknown format with 400`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/1/export?format=pdf")
            .then()
            .statusCode(400)
    }

    @Test
    fun `export returns 404 for a non-existent version`() {
        buildRealisticDraft(estimationId)
        given().post("/api/estimations/$estimationId/versions/draft/submit")

        given()
            .`when`().get("/api/estimations/$estimationId/versions/99/export?format=csv")
            .then()
            .statusCode(404)
    }

    @Test
    fun `draft can be exported without submitting it`() {
        buildRealisticDraft(estimationId)

        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft/export?format=csv")
            .then()
            .statusCode(200)
            .contentType("text/csv")

        // exporting the draft must not have submitted it
        given()
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then()
            .statusCode(200)
    }

    private fun buildRealisticDraft(estimationId: UUID) {
        given().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "parameters": [
                        {"name": "Tagessatz", "value": 900.0},
                        {"name": "Standardabweichungsfaktor", "value": 2.0},
                        {"name": "Vertriebszuschlag", "value": 0.10}
                    ],
                    "effortDrivers": [
                        {"description": "QA", "factor": 0.15}
                    ],
                    "itemGroups": [{
                        "title": "Development",
                        "items": [
                            {"description": "Feature A", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0},
                            {"description": "Feature B", "minEffort": 1.0, "expectedEffort": 3.0, "maxEffort": 5.0}
                        ]
                    }]
                }
            """.trimIndent())
            .put("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
    }
}
