package io.pythia.rest

import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.repository.EstimationRepository
import io.pythia.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

// Hours of a working day (task-213): the tenant setting that turns accompanying
// work's hours into person-days, and its recording on submitted versions.
//
// Like SystemSettingsResourceIT, every request carries its own `Authorization:
// Dev <subject>` header, and the setting is GLOBAL — so @AfterEach puts 8 back,
// or every later test that calculates accompanying work would silently see it.
@QuarkusTest
class SystemSettingsHoursPerDayIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    private val admin = "Dev dev-admin"
    private val estimator = "Dev dev-estimator"
    private lateinit var estimationId: UUID

    /** 1/2/3 h/week over 4 weeks = 8 hours; no risk surcharge, so offer-PT = 8 / hoursPerDay. */
    private val draftWithAccompanyingWork = """
        {
            "phases": [{"name": "Analysis", "abbreviation": "AN", "durationWeeks": 4.0}],
            "stdDevFactor": 0.0,
            "roots": [{"type": "GROUP", "title": "G", "children": [{
                "type": "TIME_RELATIVE",
                "description": "Projektbegleitung",
                "unit": "h/Woche",
                "minEffort": 1.0,
                "expectedEffort": 2.0,
                "maxEffort": 3.0,
                "phaseAbbreviation": "AN"
            }]}]
        }
    """.trimIndent()

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Hours per day" }
        projectRepository.persist(project)
        val estimation = Estimation().apply {
            offer = "HPD-${UUID.randomUUID()}"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @AfterEach
    fun resetHoursPerDay() = setHoursPerDay(8.0)

    private fun setHoursPerDay(hours: Double, auth: String = admin, expectedStatus: Int = 204) {
        given().header("Authorization", auth)
            .contentType(ContentType.JSON).body("""{"hoursPerDay": $hours}""")
            .`when`().put("/api/system/hours-per-day")
            .then().statusCode(expectedStatus)
    }

    private fun seedDraft() {
        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions").then().statusCode(201)
        given().header("Authorization", admin)
            .contentType(ContentType.JSON).body(draftWithAccompanyingWork)
            .`when`().put("/api/estimations/$estimationId/versions/draft").then().statusCode(200)
    }

    @Test
    fun `the default is 8 and readable anonymously`() {
        given().`when`().get("/api/system")
            .then().statusCode(200)
            .body("hoursPerDay", equalTo(8.0f))
    }

    @Test
    fun `an admin round-trips the hours per day`() {
        setHoursPerDay(7.5)
        given().`when`().get("/api/system")
            .then().statusCode(200)
            .body("hoursPerDay", equalTo(7.5f))
    }

    @Test
    fun `values outside 0 to 24 are rejected — 400`() {
        setHoursPerDay(0.0, expectedStatus = 400)
        setHoursPerDay(-1.0, expectedStatus = 400)
        setHoursPerDay(25.0, expectedStatus = 400)
        given().`when`().get("/api/system").then().body("hoursPerDay", equalTo(8.0f))
    }

    @Test
    fun `a non-admin cannot set it — 403`() {
        setHoursPerDay(6.0, auth = estimator, expectedStatus = 403)
    }

    @Test
    fun `a draft is calculated with the current setting`() {
        seedDraft()
        given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("hoursPerDay", equalTo(8.0f))
            .body("roots[0].children[0].offerPT", equalTo(1.0f))

        setHoursPerDay(4.0)
        given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("hoursPerDay", equalTo(4.0f))
            .body("roots[0].children[0].offerPT", equalTo(2.0f))
    }

    @Test
    fun `a submitted version keeps the hours per day and the numbers it was calculated with`() {
        setHoursPerDay(7.5)
        seedDraft()
        given().header("Authorization", admin)
            .post("/api/estimations/$estimationId/versions/draft/submit").then().statusCode(200)

        setHoursPerDay(8.0)

        // The snapshot is unchanged by the later setting: 8 h / 7.5 h per day.
        val submitted = given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/1")
            .then().statusCode(200)
            .body("hoursPerDay", equalTo(7.5f))
            .extract().jsonPath()
        assertEquals(8.0 / 7.5, submitted.getDouble("roots[0].children[0].offerPT"), 1e-4)

        // A draft continued from it is calculated with the CURRENT setting…
        given().header("Authorization", admin)
            .post("/api/estimations/$estimationId/versions").then().statusCode(201)
            .body("hoursPerDay", equalTo(8.0f))
            .body("roots[0].children[0].offerPT", equalTo(1.0f))

        // …and the comparison says why the numbers differ.
        given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/1/compare/draft")
            .then().statusCode(200)
            .body("parameterChanges.name", hasItem("hoursPerDay"))
            .body("parameterChanges.find { it.name == 'hoursPerDay' }.oldValue", equalTo(7.5f))
            .body("parameterChanges.find { it.name == 'hoursPerDay' }.newValue", equalTo(8.0f))
    }
}
