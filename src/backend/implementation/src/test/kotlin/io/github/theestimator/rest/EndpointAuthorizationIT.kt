package io.github.theestimator.rest

import io.github.theestimator.repository.UserRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// Deliberately does NOT use the DevAdminAuth extension: each request sets (or
// omits) its own `Authorization: Dev <subject>` header so the authorization
// contract can be asserted per role. This is the regression guard for the
// "unauthenticated requests silently succeed" class of bug.
@QuarkusTest
class EndpointAuthorizationIT {

    @Inject
    lateinit var userRepository: UserRepository

    @Test
    fun `write without auth is 401`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"NoAuth"}""")
            .`when`().post("/api/projects")
            .then().statusCode(401)
    }

    @Test
    fun `write as viewer is 403`() {
        given()
            .header("Authorization", "Dev dev-viewer")
            .contentType(ContentType.JSON)
            .body("""{"name":"ViewerWrite"}""")
            .`when`().post("/api/projects")
            .then().statusCode(403)
    }

    @Test
    @Transactional
    fun `write as estimator is 201 and provisions the user`() {
        given()
            .header("Authorization", "Dev dev-estimator")
            .contentType(ContentType.JSON)
            .body("""{"name":"EstimatorWrite"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201)

        assertNotNull(
            userRepository.findByEntraSubjectId("dev-estimator"),
            "an authenticated write must provision the acting user"
        )
    }

    @Test
    fun `read without auth is 401`() {
        given()
            .`when`().get("/api/projects")
            .then().statusCode(401)
    }

    @Test
    fun `read as viewer is 200`() {
        given()
            .header("Authorization", "Dev dev-viewer")
            .`when`().get("/api/projects")
            .then().statusCode(200)
    }

    @Test
    fun `create estimation as viewer is 403`() {
        given()
            .header("Authorization", "Dev dev-viewer")
            .contentType(ContentType.JSON)
            .body("""{"offer":"VIEWER-EST"}""")
            .`when`().post("/api/projects/${java.util.UUID.randomUUID()}/estimations")
            .then().statusCode(403)
    }
}
