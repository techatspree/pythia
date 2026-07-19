package io.github.theestimator.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

// Pins the JSON error-body shape returned by the authorization ExceptionMappers
// (task-093). Does NOT use the DevAdminAuth extension — it drives the denied
// paths explicitly: dev-viewer (VIEWER role) for a 403 write, and no header for
// a 401.
@QuarkusTest
class AuthorizationErrorIT {

    @Test
    fun `a VIEWER write is denied with a JSON 403 body`() {
        given()
            .header("Authorization", "Dev dev-viewer")
            .contentType(ContentType.JSON)
            .body("""{"name":"Denied"}""")
            .`when`().post("/api/projects")
            .then()
            .statusCode(403)
            .contentType(ContentType.JSON)
            .body("message", not(blankOrNullString()))
            .body("status", equalTo(403))
    }

    @Test
    fun `an anonymous request is denied with a JSON 401 body`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Denied"}""")
            .`when`().post("/api/projects")
            .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("message", not(blankOrNullString()))
            .body("status", equalTo(401))
    }
}
