package io.github.theestimator.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import java.util.UUID

// Bucket + sampled collaborative session (task-106). Like SessionResourceIT this
// is a multi-user flow, so each request carries its own `Authorization: Dev …`
// header rather than the global DevAdminAuth spec. dev-admin moderates;
// dev-estimator and dev-viewer-estimator are the other two estimators.
@QuarkusTest
class BucketSessionResourceIT {

    private val moderator = "Dev dev-admin"
    private val estimator2 = "Dev dev-estimator"

    private data class Fixture(
        val estimationId: String,
        val bucketSmall: String,
        val bucketLarge: String,
        val sampleLeaf: String,
        val nonSampleLeaf: String
    )

    private fun createBucketDraft(): Fixture {
        val projectId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"name":"Bucket Session Project"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")

        val estimationId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"offer":"BUCKET-SESSION","method":"BUCKET_SAMPLED_PERT"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", moderator)
            .`when`().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)

        val small = UUID.randomUUID().toString()
        val large = UUID.randomUUID().toString()
        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "stdDevFactor": 0.0,
                    "buckets": [
                        {"id": "$small", "position": 0, "label": "S"},
                        {"id": "$large", "position": 1, "label": "L"}
                    ],
                    "roots": [
                        {"type": "BUCKETED", "description": "To sample", "bucketId": "$small", "isSample": false},
                        {"type": "BUCKETED", "description": "Rides the bucket", "bucketId": "$small", "isSample": false}
                    ]
                }
                """.trimIndent()
            )
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)

        val draft = given().header("Authorization", moderator)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200).extract()

        return Fixture(
            estimationId, small, large,
            draft.path("roots[0].logicalId"), draft.path("roots[1].logicalId")
        )
    }

    private fun startSession(f: Fixture, items: List<String>): String {
        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body(
                """
                {"estimationId":"${f.estimationId}","title":"Bucket round",
                 "itemLogicalIds":${items.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}
                """.trimIndent()
            )
            .`when`().post("/api/sessions")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", estimator2).`when`().post("/api/sessions/$sessionId/join")
            .then().statusCode(200)
        given().header("Authorization", moderator).`when`().post("/api/sessions/$sessionId/start")
            .then().statusCode(200)
        return sessionId
    }

    private fun bucketVote(
        sessionId: String,
        auth: String,
        bucketId: String,
        isSample: Boolean = false,
        min: Double = 0.0,
        expected: Double = 0.0,
        max: Double = 0.0
    ) {
        given().header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(
                """{"bucketId":"$bucketId","isSample":$isSample,
                    "minEffort":$min,"expectedEffort":$expected,"maxEffort":$max}"""
            )
            .`when`().post("/api/sessions/$sessionId/votes/bucket")
            .then().statusCode(200)
    }

    @Test
    fun `bucket session reduces to the last-written bucket and reports the loser as a conflict`() {
        val f = createBucketDraft()
        val sessionId = startSession(f, listOf(f.sampleLeaf))

        // Two estimators disagree; the moderator writes S first, dev-estimator L
        // second, so LWW must land on L and report S as a conflict.
        bucketVote(sessionId, moderator, f.bucketSmall)
        bucketVote(sessionId, estimator2, f.bucketLarge, isSample = true, min = 2.0, expected = 4.0, max = 6.0)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2")
            .then().statusCode(200)
            .body("items[0].bucketAssignment.bucketId", equalTo(f.bucketLarge))
            .body("items[0].bucketAssignment.source", equalTo("dev-estimator"))
            .body("items[0].bucketAssignment.conflictingAssignments", hasSize<Any>(1))
            .body("items[0].bucketAssignment.conflictingAssignments[0].bucketId", equalTo(f.bucketSmall))
            .body("items[0].bucketAssignment.conflictingAssignments[0].estimatorId", equalTo("dev-admin"))
            // A bucket session carries no PERT aggregate.
            .body("items[0].aggregate", equalTo(null))
    }

    @Test
    fun `agreement on one bucket produces no conflicts`() {
        val f = createBucketDraft()
        val sessionId = startSession(f, listOf(f.sampleLeaf))

        bucketVote(sessionId, moderator, f.bucketSmall)
        bucketVote(sessionId, estimator2, f.bucketSmall)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2")
            .then().statusCode(200)
            .body("items[0].bucketAssignment.bucketId", equalTo(f.bucketSmall))
            .body("items[0].bucketAssignment.conflictingAssignments", hasSize<Any>(0))
    }

    @Test
    fun `finalize writes the agreed bucket and averaged sample back onto the draft leaf`() {
        val f = createBucketDraft()
        val sessionId = startSession(f, listOf(f.sampleLeaf))

        // Both sample the same bucket: (1,2,3) and (3,4,5) average to (2,3,4).
        bucketVote(sessionId, moderator, f.bucketLarge, isSample = true, min = 1.0, expected = 2.0, max = 3.0)
        bucketVote(sessionId, estimator2, f.bucketLarge, isSample = true, min = 3.0, expected = 4.0, max = 5.0)

        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(200)

        given().header("Authorization", moderator)
            .`when`().get("/api/estimations/${f.estimationId}/versions/draft")
            .then().statusCode(200)
            .body("roots[0].bucketId", equalTo(f.bucketLarge))
            .body("roots[0].isSample", equalTo(true))
            .body("roots[0].minEffort", equalTo(2.0f))
            .body("roots[0].expectedEffort", equalTo(3.0f))
            .body("roots[0].maxEffort", equalTo(4.0f))
    }

    @Test
    fun `a non-sample leaf derives its effort from the bucket's samples after finalize`() {
        val f = createBucketDraft()
        // Session over BOTH leaves: the first becomes the bucket's sample, the
        // second is finalized as a non-sample in the same bucket and must then
        // inherit the bucket average through EstimationVersion.calculate().
        val sessionId = startSession(f, listOf(f.sampleLeaf, f.nonSampleLeaf))

        bucketVote(sessionId, moderator, f.bucketSmall, isSample = true, min = 2.0, expected = 4.0, max = 6.0)
        bucketVote(sessionId, estimator2, f.bucketSmall, isSample = true, min = 2.0, expected = 4.0, max = 6.0)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(200)

        // Second item: same bucket, nobody samples it.
        bucketVote(sessionId, moderator, f.bucketSmall)
        bucketVote(sessionId, estimator2, f.bucketSmall)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/phase2").then().statusCode(200)
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/items/current/finalize").then().statusCode(200)

        // PERT(2,4,6) = 4.0, and with stdDevFactor 0 the risk factor is 0, so the
        // non-sample leaf's mean is its bucket's sample mean.
        given().header("Authorization", moderator)
            .`when`().get("/api/estimations/${f.estimationId}/versions/draft")
            .then().statusCode(200)
            .body("roots[1].isSample", equalTo(false))
            .body("roots[1].mean", equalTo(4.0f))
    }

    @Test
    fun `a bucket vote against a PERT estimation is rejected with 409`() {
        val projectId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"name":"PERT Project"}""")
            .`when`().post("/api/projects").then().statusCode(201).extract().path<String>("id")
        val estimationId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"offer":"PERT-EST"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")
        given().header("Authorization", moderator)
            .`when`().post("/api/estimations/$estimationId/versions").then().statusCode(201)
        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"roots":[{"type":"FIXED","description":"L","minEffort":1.0,"expectedEffort":2.0,"maxEffort":3.0}]}""")
            .`when`().put("/api/estimations/$estimationId/versions/draft").then().statusCode(200)
        val leaf = given().header("Authorization", moderator)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200).extract().path<String>("roots[0].logicalId")

        val sessionId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"estimationId":"$estimationId","title":"PERT round","itemLogicalIds":["$leaf"]}""")
            .`when`().post("/api/sessions").then().statusCode(201).extract().path<String>("id")
        given().header("Authorization", moderator)
            .`when`().post("/api/sessions/$sessionId/start").then().statusCode(200)

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"bucketId":"${UUID.randomUUID()}","isSample":false}""")
            .`when`().post("/api/sessions/$sessionId/votes/bucket")
            .then().statusCode(409)
    }

    @Test
    fun `a bucket from another estimation is rejected with 409`() {
        val f = createBucketDraft()
        val sessionId = startSession(f, listOf(f.sampleLeaf))

        given().header("Authorization", moderator)
            .contentType(ContentType.JSON)
            .body("""{"bucketId":"${UUID.randomUUID()}","isSample":false}""")
            .`when`().post("/api/sessions/$sessionId/votes/bucket")
            .then().statusCode(409)
    }
}
