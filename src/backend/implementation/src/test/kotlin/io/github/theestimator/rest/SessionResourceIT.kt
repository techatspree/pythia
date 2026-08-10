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
    fun `a moderate-only moderator cannot vote — 409`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body(
                """{"estimationId":"${draft.estimationId}","title":"t","itemLogicalIds":["${draft.leaf1}"],"moderatorEstimates":false}"""
            )
            .`when`().post("/api/sessions").then().statusCode(201)
            .body("moderatorEstimates", equalTo(false))
            .extract().path<String>("id")
        given().header("Authorization", moderator).post("/api/sessions/$sessionId/start").then().statusCode(200)

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"minEffort":1.0,"expectedEffort":2.0,"maxEffort":3.0}""")
            .`when`().post("/api/sessions/$sessionId/votes")
            .then().statusCode(409)
    }

    @Test
    fun `finalize appends the session notes to the leaf assumptions`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"${draft.estimationId}","title":"Delphi run","itemLogicalIds":["${draft.leaf1}"]}""")
            .`when`().post("/api/sessions").then().statusCode(201).extract().path<String>("id")
        given().header("Authorization", moderator).post("/api/sessions/$sessionId/start").then().statusCode(200)

        vote(sessionId, moderator, 2.0, 4.0, 6.0)

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"notes":"Assume the SSO service is available"}""")
            .`when`().put("/api/sessions/$sessionId/items/current/notes")
            .then().statusCode(200)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(200)

        val assumptions = given().header("Authorization", moderator)
            .`when`().get("/api/estimations/${draft.estimationId}/versions/draft")
            .then().statusCode(200).extract().path<String>("roots[0].assumptions")
        org.junit.jupiter.api.Assertions.assertTrue(
            assumptions.contains("Delphi run") && assumptions.contains("Assume the SSO service is available"),
            "assumptions should carry the session name + notes, was: $assumptions"
        )
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

    // --- task-144: suspend / resume / end-early -----------------------------

    @Test
    fun `suspend parks a running session and resume restores it exactly`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = startedSession(draft, "Parked", listOf(draft.leaf1, draft.leaf2))

        // Get the room into a non-trivial position first: item 0 revealed to PHASE2.
        vote(sessionId, moderator, 2.0, 4.0, 6.0)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/suspend")
            .then().statusCode(200).body("status", equalTo("SUSPENDED"))

        // Nothing about the position moved while parked.
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/resume")
            .then().statusCode(200)
            .body("status", equalTo("RUNNING"))
            .body("currentItemIndex", equalTo(0))
            .body("currentPhase", equalTo("PHASE2"))
            .body("items[0].status", equalTo("PHASE2"))
            .body("items[1].status", equalTo("PENDING"))
            .body("items[0].votes.size()", equalTo(1))
    }

    @Test
    fun `a suspended session accepts no vote, reveal or finalize — 409`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = startedSession(draft, "Inert", listOf(draft.leaf1))
        vote(sessionId, moderator, 2.0, 4.0, 6.0)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/suspend").then().statusCode(200)

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"minEffort":1.0,"expectedEffort":2.0,"maxEffort":3.0}""")
            .`when`().post("/api/sessions/$sessionId/votes").then().statusCode(409)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(409)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(409)
    }

    @Test
    fun `wrong-state transitions are 409`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = createSession(draft, "States", listOf(draft.leaf1))

        // CREATED holds nothing worth parking.
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/suspend").then().statusCode(409)

        given().header("Authorization", moderator).post("/api/sessions/$sessionId/start").then().statusCode(200)
        // Already running.
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/resume").then().statusCode(409)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/end-early")
            .then().statusCode(200).body("status", equalTo("ENDED_EARLY"))
        // ENDED_EARLY is terminal.
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/resume").then().statusCode(409)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/suspend").then().statusCode(409)
    }

    @Test
    fun `suspend, resume and end-early are moderator-only — 403`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = startedSession(draft, "Guarded", listOf(draft.leaf1))
        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/join").then().statusCode(200)

        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/suspend").then().statusCode(403)
        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/end-early").then().statusCode(403)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/suspend").then().statusCode(200)
        given().header("Authorization", estimator)
            .`when`().post("/api/sessions/$sessionId/resume").then().statusCode(403)
    }

    @Test
    fun `ending early keeps the write-back of every finalized item and leaves the rest untouched`() {
        val draft = createDraftWithTwoLeaves()
        val sessionId = startedSession(draft, "Out of time", listOf(draft.leaf1, draft.leaf2))

        // Item 1 goes the whole way — its triple reaches the draft leaf immediately.
        vote(sessionId, moderator, 2.0, 4.0, 6.0)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(200)

        // Item 2 is only half-voted when the clock runs out.
        vote(sessionId, moderator, 10.0, 20.0, 30.0)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/end-early")
            .then().statusCode(200)
            .body("status", equalTo("ENDED_EARLY"))
            .body("items[0].status", equalTo("FINALIZED"))
            .body("items[0].finalTriple.expectedEffort", equalTo(4.0f))
            // No implicit finalize: the in-flight item keeps no final estimate.
            .body("items[1].finalTriple", org.hamcrest.Matchers.nullValue())

        given().header("Authorization", moderator)
            .`when`().get("/api/estimations/${draft.estimationId}/versions/draft")
            .then().statusCode(200)
            // Finalized before the early end → saved.
            .body("roots[0].minEffort", equalTo(2.0f))
            .body("roots[0].expectedEffort", equalTo(4.0f))
            .body("roots[0].maxEffort", equalTo(6.0f))
            // Never finalized → the leaf keeps its original values.
            .body("roots[1].expectedEffort", equalTo(2.0f))
    }

    @Test
    fun `the joinable list keeps a suspended session and drops an ended-early one`() {
        val draft = createDraftWithTwoLeaves()
        val parked = startedSession(draft, "Parked room", listOf(draft.leaf1))
        val over = startedSession(draft, "Finished room", listOf(draft.leaf2))

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$parked/suspend").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$over/end-early").then().statusCode(200)

        given().header("Authorization", estimator)
            .`when`().get("/api/sessions")
            .then().statusCode(200)
            .body("find { it.id == '$parked' }.status", equalTo("SUSPENDED"))
            .body("findAll { it.id == '$over' }.size()", equalTo(0))
    }

    private fun createSession(draft: Draft, title: String, leaves: List<String>): String =
        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body(
                """{"estimationId":"${draft.estimationId}","title":"$title","itemLogicalIds":[${
                    leaves.joinToString(",") { "\"$it\"" }
                }]}"""
            )
            .`when`().post("/api/sessions")
            .then().statusCode(201).extract().path("id")

    private fun startedSession(draft: Draft, title: String, leaves: List<String>): String {
        val id = createSession(draft, title, leaves)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$id/start").then().statusCode(200)
        return id
    }

    private fun vote(sessionId: String, auth: String, min: Double, expected: Double, max: Double) {
        given().header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body("""{"minEffort":$min,"expectedEffort":$expected,"maxEffort":$max}""")
            .`when`().post("/api/sessions/$sessionId/votes")
            .then().statusCode(200)
    }
}
