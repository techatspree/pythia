package io.pythia.observability

import io.pythia.auth.DevAdminAuth
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

// Proves the observability wiring (task-026): the CorrelationIdFilter round-trips
// the X-Correlation-ID header, and Micrometer's Prometheus endpoint is served.
@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class CorrelationIdFilterIT {

    @Test
    fun `echoes back an incoming X-Correlation-ID`() {
        given().header("X-Correlation-ID", "test-corr-123")
            .`when`().get("/api/auth/me")
            .then().statusCode(200)
            .header("X-Correlation-ID", equalTo("test-corr-123"))
    }

    @Test
    fun `generates an X-Correlation-ID when the request omits one`() {
        given()
            .`when`().get("/api/auth/me")
            .then().statusCode(200)
            .header("X-Correlation-ID", not(emptyOrNullString()))
    }

    @Test
    fun `exposes Prometheus http server metrics`() {
        // Make an application request so the http_server_requests timer is recorded.
        given().`when`().get("/api/auth/me").then().statusCode(200)

        given().`when`().get("/q/metrics")
            .then().statusCode(200)
            .body(containsString("http_server_requests_seconds_count"))
    }
}
