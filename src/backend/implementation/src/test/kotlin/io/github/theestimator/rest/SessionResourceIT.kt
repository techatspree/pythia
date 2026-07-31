package io.github.theestimator.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

// Multi-user flow, so this does NOT use the global DevAdminAuth spec — each
// request carries its own `Authorization: Dev <subject>` header. dev-admin is
// the moderator; dev-estimator is a second estimator (both have ESTIMATOR).
@QuarkusTest
class SessionResourceIT {

    private val moderator = "Dev dev-admin"
    private val estimator = "Dev dev-estimator"

    private data class Draft(val estimationId: String, val leaf1: String, val leaf2: String)

    private fun createDraftWithTwoLeaves(): Draft {
        val projectId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"name":"Session Project"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")

        val estimationId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"offer":"SESSION-EST"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", moderator)
            .`when`().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body(
                """
                {"roots": [
                    {"type": "FIXED", "description": "Leaf A", "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0},
                    {"type": "FIXED", "description": "Leaf B", "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0}
                ]}
                """.trimIndent()
            )
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)

        val draftJson = given().header("Authorization", moderator)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200).extract()
        return Draft(
            estimationId,
            draftJson.path("roots[0].logicalId"),
            draftJson.path("roots[1].logicalId")
        )
    }

    @Test
    fun `full happy path — create, vote, reveal, revise, finalize writes back and advances`() {
        val draft = createDraftWithTwoLeaves()

        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"${draft.estimationId}","title":"Sprint sizing","itemLogicalIds":["${draft.leaf1}","${draft.leaf2}"]}""")
            .`when`().post("/api/sessions")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/join")
            .then().statusCode(200)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/start")
            .then().statusCode(200).body("status", equalTo("RUNNING"))

        // PHASE1 blind votes — deliberately divergent on expectedEffort.
        vote(sessionId, moderator, 2.0, 4.0, 6.0)
        vote(sessionId, estimator, 8.0, 12.0, 16.0)

        // Reveal: aggregate is visible and flagged diverged.
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2")
            .then().statusCode(200)
            .body("items[0].aggregate.diverged", equalTo(true))
            .body("items[0].votes.size()", equalTo(2))

        // PHASE2 revision by the estimator; finalize then uses PHASE2 votes.
        vote(sessionId, estimator, 2.0, 4.0, 6.0)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize")
            .then().statusCode(200)
            .body("currentItemIndex", equalTo(1))
            .body("items[0].status", equalTo("FINALIZED"))

        // The finalized mean triple (2/4/6) is written back onto the draft leaf.
        given().header("Authorization", moderator)
            .`when`().get("/api/estimations/${draft.estimationId}/versions/draft")
            .then().statusCode(200)
            .body("roots[0].minEffort", equalTo(2.0f))
            .body("roots[0].expectedEffort", equalTo(4.0f))
            .body("roots[0].maxEffort", equalTo(6.0f))
    }

    @Test
    fun `GET sessions without estimationId lists joinable sessions`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"${draft.estimationId}","title":"Joinable","itemLogicalIds":["${draft.leaf1}"]}""")
            .`when`().post("/api/sessions").then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", estimator)
            .`when`().get("/api/sessions")
            .then().statusCode(200)
            .body("find { it.id == '$sessionId' }.title", equalTo("Joinable"))
    }

    @Test
    fun `a non-moderator finalize is 403`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"${draft.estimationId}","title":"t","itemLogicalIds":["${draft.leaf1}"]}""")
            .`when`().post("/api/sessions").then().statusCode(201).extract().path<String>("id")
        given().header("Authorization", moderator).post("/api/sessions/$sessionId/start").then().statusCode(200)

        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize")
            .then().statusCode(403)
    }

    @Test
    fun `voting before the session is running is 409`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"${draft.estimationId}","title":"t","itemLogicalIds":["${draft.leaf1}"]}""")
            .`when`().post("/api/sessions").then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"minEffort":1.0,"expectedEffort":2.0,"maxEffort":3.0}""")
            .`when`().post("/api/sessions/$sessionId/votes")
            .then().statusCode(409)
    }

    private fun vote(sessionId: String, auth: String, min: Double, expected: Double, max: Double) {
        given().header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body("""{"minEffort":$min,"expectedEffort":$expected,"maxEffort":$max}""")
            .`when`().post("/api/sessions/$sessionId/votes")
            .then().statusCode(200)
    }
}
