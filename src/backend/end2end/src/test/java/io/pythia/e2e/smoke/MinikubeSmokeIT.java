package io.pythia.e2e.smoke;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Read-only smoke suite against a running stack (task-087): GET requests only,
 * and nothing that assumes data exists. Run it with
 * {@code ./gradlew :backend:end2end:smokeTest} or {@code ./scripts/minikube-smoke.sh};
 * it is never part of {@code ./gradlew build}.
 */
@Tag("smoke")
class MinikubeSmokeIT {

    private static final String BACKEND = System.getProperty("backend.url", "http://localhost:18080");
    private static final String FRONTEND = System.getProperty("frontend.url", "http://localhost:18081");
    private static final String TOKEN = System.getenv("SMOKE_TOKEN");

    @Test
    void healthIsUp() {
        given().baseUri(BACKEND).when().get("/q/health")
            .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void readinessIsUp() {
        given().baseUri(BACKEND).when().get("/q/health/ready")
            .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void livenessIsUp() {
        given().baseUri(BACKEND).when().get("/q/health/live")
            .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void openApiIsServed() {
        given().baseUri(BACKEND).when().get("/q/openapi")
            .then().statusCode(200);
    }

    @Test
    void anonymousApiCallIsRejected() {
        given().baseUri(BACKEND).when().get("/api/projects")
            .then().statusCode(401);
    }

    @Test
    void frontendServesTheSpa() {
        given().baseUri(FRONTEND).when().get("/")
            .then().statusCode(200).contentType(startsWith("text/html"));
    }

    @Test
    void frontendProxiesToTheBackendAndItsDatabase() {
        // Anonymous and backed by the settings row: the same probe as scripts/smoke.sh.
        given().baseUri(FRONTEND).when().get("/api/system")
            .then().statusCode(200).contentType(ContentType.JSON).body("hoursPerDay", notNullValue());
    }

    @Test
    void authenticatedReadReturnsAList() {
        Assumptions.assumeTrue(TOKEN != null && !TOKEN.isBlank(), "SMOKE_TOKEN not set");
        given().baseUri(BACKEND).header("Authorization", "Bearer " + TOKEN)
            .when().get("/api/projects")
            .then().statusCode(200).body("$", instanceOf(List.class));
    }
}
