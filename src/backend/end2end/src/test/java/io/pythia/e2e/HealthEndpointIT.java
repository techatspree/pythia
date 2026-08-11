package io.pythia.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class HealthEndpointIT {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = System.getProperty("backend.url", "http://localhost:8080");
    }

    @Test
    void healthEndpointReturnsUp() {
        given()
            .when()
                .get("/q/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
