package io.github.theestimator.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Drives the Merlin export endpoint end-to-end against the real sample document
// (src/test/resources/merlin/state.sql): a clean export, the 409 the user has to
// decide about once the estimation structure drifts, and the overwrite that
// resolves it.
@QuarkusTest
class MerlinExportResourceIT {

    private val user = "Dev dev-admin"

    private fun sampleBytes(): ByteArray =
        javaClass.getResourceAsStream("/merlin/state.sql")?.readBytes()
            ?: error("test resource /merlin/state.sql missing")

    private fun importedEstimation(): String {
        val projectId = given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"name":"Merlin Export Project"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")

        val estimationId = given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"offer":"MERLIN-EXPORT-1"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(201)

        return estimationId
    }

    @Test
    fun `export writes into a copy and returns it as an attachment`() {
        val estimationId = importedEstimation()

        val body = given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/draft/export/merlin")
            .then().statusCode(200)
            .header("Content-Disposition", containsString("attachment"))
            .extract().asByteArray()

        assertTrue(body.size > 0, "the exported document must not be empty")
        assertTrue(
            String(body, 0, 15, Charsets.US_ASCII) == "SQLite format 3",
            "a bare state.sql upload must come back as a SQLite document"
        )
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
            .`when`().post("/api/estimations/$estimationId/versions/draft/export/merlin")
            .then().statusCode(409)
            .body("inSync", org.hamcrest.Matchers.equalTo(false))
            .body("missingInMerlin.size()", greaterThan(0))
            .body("missingInEstimation.size()", greaterThan(0))

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post(
                "/api/estimations/$estimationId/versions/draft/export/merlin?overwriteStructure=true"
            )
            .then().statusCode(200)
    }

    @Test
    fun `an unreadable upload is rejected with 400`() {
        val estimationId = importedEstimation()

        given().header("Authorization", user)
            .multiPart("file", "junk.bin", "not a sqlite file".toByteArray(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/draft/export/merlin")
            .then().statusCode(400)
    }
}
