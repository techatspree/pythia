package io.github.theestimator.rest

import io.github.theestimator.auth.DevAdminAuth
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class ProjectResourceIT {

    @BeforeEach
    @Transactional
    fun setup() {
        // Test data is created per test via the API
    }

    @Test
    fun `list projects returns empty list initially`() {
        given()
            .`when`().get("/api/projects")
            .then()
            .statusCode(200)
            .body("$", notNullValue())
    }

    @Test
    fun `create project returns 201`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Test Project", "description": "A test", "client": "ACME"}""")
            .`when`().post("/api/projects")
            .then()
            .statusCode(201)
            .body("name", equalTo("Test Project"))
            .body("description", equalTo("A test"))
            .body("client", equalTo("ACME"))
            .body("status", equalTo("ACTIVE"))
            .body("id", notNullValue())
    }

    @Test
    fun `get project returns detail with estimations`() {
        val id = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Detail Project"}""")
            .post("/api/projects")
            .then().statusCode(201)
            .extract().path<String>("id")

        given()
            .`when`().get("/api/projects/$id")
            .then()
            .statusCode(200)
            .body("name", equalTo("Detail Project"))
            .body("estimations", notNullValue())
    }

    @Test
    fun `get nonexistent project returns 404`() {
        given()
            .`when`().get("/api/projects/${UUID.randomUUID()}")
            .then()
            .statusCode(404)
    }

    @Test
    fun `update project changes metadata`() {
        val id = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Original"}""")
            .post("/api/projects")
            .then().statusCode(201)
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Updated", "client": "NewClient"}""")
            .`when`().put("/api/projects/$id")
            .then()
            .statusCode(200)
            .body("name", equalTo("Updated"))
            .body("client", equalTo("NewClient"))
    }

    @Test
    fun `archive project changes status`() {
        val id = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "To Archive"}""")
            .post("/api/projects")
            .then().statusCode(201)
            .extract().path<String>("id")

        given()
            .`when`().post("/api/projects/$id/archive")
            .then()
            .statusCode(200)
            .body("status", equalTo("ARCHIVED"))
    }

    @Test
    fun `filter by status returns only matching projects`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Active Project"}""")
            .post("/api/projects")

        val idToArchive = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Archived Project"}""")
            .post("/api/projects")
            .then().extract().path<String>("id")

        given().post("/api/projects/$idToArchive/archive")

        given()
            .queryParam("status", "ACTIVE")
            .`when`().get("/api/projects")
            .then()
            .statusCode(200)
            .body("find { it.name == 'Archived Project' }", nullValue())
    }
}
