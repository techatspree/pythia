package io.pythia.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

// Drives the Merlin WBS import endpoint end-to-end with the real sample document
// (src/test/resources/merlin/state.sql). Uses explicit Dev auth headers.
@QuarkusTest
class MerlinImportResourceIT {

    private val user = "Dev dev-admin"

    private fun sampleBytes(): ByteArray =
        javaClass.getResourceAsStream("/merlin/state.sql")?.readBytes()
            ?: error("test resource /merlin/state.sql missing")

    private fun createEstimation(): String {
        val projectId = given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"name":"Merlin Project"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")

        return given().header("Authorization", user)
            .contentType(ContentType.JSON).body("""{"offer":"MERLIN-001"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")
    }

    @Test
    fun `import a Merlin WBS creates a draft, second import conflicts`() {
        val estimationId = createEstimation()

        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(201)
            .body("isDraft", equalTo(true))
            .body("roots.size()", greaterThan(0))

        given().header("Authorization", user)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("roots.size()", greaterThan(0))

        // A draft now exists → a second import is a conflict.
        given().header("Authorization", user)
            .multiPart("file", "state.sql", sampleBytes(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(409)
    }

    @Test
    fun `an unreadable upload is rejected with 400`() {
        val estimationId = createEstimation()

        given().header("Authorization", user)
            .multiPart("file", "junk.bin", "not a sqlite file".toByteArray(), "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(400)
    }
}
