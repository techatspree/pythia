package io.pythia.rest

import io.pythia.auth.DevAdminAuth
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.oneOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class MeResourceLanguageIT {

    @Test
    fun `me exposes a language preference`() {
        given()
            .`when`().get("/api/auth/me")
            .then()
            .statusCode(200)
            .body("language", oneOf("de", "en"))
    }

    @Test
    fun `put language updates the stored preference`() {
        given().contentType(ContentType.JSON).body("""{"language":"en"}""")
            .`when`().put("/api/auth/me/language")
            .then().statusCode(204)

        given().`when`().get("/api/auth/me")
            .then().statusCode(200).body("language", equalTo("en"))

        // Reset so the preference does not leak into other tests/classes.
        given().contentType(ContentType.JSON).body("""{"language":"de"}""")
            .`when`().put("/api/auth/me/language")
            .then().statusCode(204)

        given().`when`().get("/api/auth/me")
            .then().statusCode(200).body("language", equalTo("de"))
    }

    @Test
    fun `put language rejects an unsupported code`() {
        given().contentType(ContentType.JSON).body("""{"language":"xx"}""")
            .`when`().put("/api/auth/me/language")
            .then().statusCode(400)
    }
}
