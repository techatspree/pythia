package io.github.theestimator.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

// The liveness heartbeat must be reachable WITHOUT authentication (task-136), so
// the frontend connection watchdog can probe reconnection regardless of sign-in
// state. No Authorization header here on purpose.
@QuarkusTest
class PingResourceIT {

    @Test
    fun `ping is reachable anonymously and returns ok`() {
        given()
            .`when`().get("/api/ping")
            .then().statusCode(200)
            .body("status", equalTo("ok"))
    }
}
