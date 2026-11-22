package io.pythia.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

// Drives the merge-into-Merlin endpoint end-to-end against the real sample
// document (src/test/resources/merlin/state.sql): a clean merge, the 409 the user
// has to decide about once the estimation structure drifts, and the overwrite
// that resolves it (task-133, renamed and given a JSON result in task-223).
@QuarkusTest
class MerlinMergeResourceIT {

    private val user = "Dev dev-admin"

    private fun sampleBytes(): ByteArray =
        javaClass.getResourceAsStream("/merlin/state.sql")?.readBytes()
            ?: error("test resource /merlin/state.sql missing")

    private fun importedEstimation(): String {
        val projectId = given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"name":"Merlin Merge Project"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")

        val estimationId = given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"offer":"MERLIN-MERGE-1"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(201)

        return estimationId
    }

    @Test
    fun `merge writes into a copy and returns it with a report`() {
        val estimationId = importedEstimation()

        val response = given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/draft/merge/merlin")
            .then().statusCode(200)
            .contentType(ContentType.JSON)
            .body("fileName", endsWith("-merged.sql"))
            .body("report", notNullValue())
            .body("report.effortChanges", notNullValue())
            // The imported draft carries the sample's two edges: nothing to add.
            .body("report.dependenciesAdded.size()", equalTo(0))
            .extract()
        val body = Base64.getDecoder().decode(response.path<String>("document"))

        assertTrue(body.size > 0, "the merged document must not be empty")
        assertTrue(
            String(body, 0, 15, Charsets.US_ASCII) == "SQLite format 3",
            "a bare state.sql upload must come back as a SQLite document"
        )
    }

    @Test
    fun `the old export path is gone`() {
        val estimationId = importedEstimation()

        val status = given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            // Split so the task-223 rename grep finds only live references.
            .`when`().post("/api/estimations/$estimationId/versions/draft/export" + "/merlin")
            .then().extract().statusCode()

        assertTrue(status == 404 || status == 405, "expected 404 or 405, was $status")
    }

    @Test
    fun `a drifted structure is refused with 409 and accepted with overwriteStructure`() {
        val estimationId = importedEstimation()

        // Drift the estimation: append a root-level item the Merlin document
        // does not have.
        val draft = given().header("Authorization", user)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200).extract().body().asString()
        assertTrue(draft.isNotEmpty())

        given().header("Authorization", user)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "roots": [
                        {"type": "FIXED", "description": "Added after the import",
                         "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0}
                    ]
                }
                """.trimIndent()
            )
            .`when`().put("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("roots.size()", greaterThan(0))

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/draft/merge/merlin")
            .then().statusCode(409)
            .body("inSync", equalTo(false))
            .body("missingInMerlin.size()", greaterThan(0))
            .body("missingInEstimation.size()", greaterThan(0))

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post(
                "/api/estimations/$estimationId/versions/draft/merge/merlin?overwriteStructure=true"
            )
            .then().statusCode(200)
            .body("report.activitiesAdded.size()", greaterThan(0))
            .body("report.activitiesRemoved.size()", greaterThan(0))
    }

    @Test
    fun `an unreadable upload is rejected with 400`() {
        val estimationId = importedEstimation()

        given().header("Authorization", user)
            .multiPart("file", "junk.bin", "not a sqlite file".toByteArray(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/draft/merge/merlin")
            .then().statusCode(400)
    }
}
