package io.pythia.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

// Per-installation system settings (task-146). Like SessionResourceIT this does
// NOT use the global DevAdminAuth spec — each request carries its own
// `Authorization: Dev <subject>` header, and one case is deliberately anonymous.
//
// These settings are GLOBAL (one singleton row + one driver template shared by
// the whole database), so every test here resets them in @AfterEach. Without
// that, a leftover driver template would silently seed every draft created by
// EstimationVersionResourceIT and friends.
@QuarkusTest
class SystemSettingsResourceIT {

    private val admin = "Dev dev-admin"
    private val estimator = "Dev dev-estimator"

    @AfterEach
    fun resetSystemSettings() {
        given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"displayName":null}""")
            .`when`().put("/api/system").then().statusCode(204)
        given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("[]")
            .`when`().put("/api/system/effort-drivers").then().statusCode(200)
        given().header("Authorization", admin)
            .`when`().delete("/api/system/css").then().statusCode(204)
    }

    @Test
    fun `GET api system is readable anonymously — the login screen depends on it`() {
        given()
            .`when`().get("/api/system")
            .then().statusCode(200)
            .body("hasCustomCss", equalTo(false))
    }

    @Test
    fun `a non-admin cannot write any system setting — 403`() {
        given().header("Authorization", estimator)
            .contentType(ContentType.JSON).body("""{"displayName":"Nope"}""")
            .`when`().put("/api/system").then().statusCode(403)

        given().header("Authorization", estimator)
            .contentType(ContentType.JSON).body("[]")
            .`when`().put("/api/system/effort-drivers").then().statusCode(403)

        given().header("Authorization", estimator)
            .`when`().delete("/api/system/css").then().statusCode(403)
    }

    @Test
    fun `an admin round-trips the display name, and a blank name reads back as null`() {
        given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"displayName":"Contoso GmbH"}""")
            .`when`().put("/api/system").then().statusCode(204)

        given().`when`().get("/api/system")
            .then().statusCode(200).body("displayName", equalTo("Contoso GmbH"))

        given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"displayName":"   "}""")
            .`when`().put("/api/system").then().statusCode(204)

        given().`when`().get("/api/system")
            .then().statusCode(200).body("displayName", nullValue())
    }

    @Test
    fun `the custom stylesheet has a 404 - upload - 404 lifecycle`() {
        given().`when`().get("/api/system/css").then().statusCode(404)

        given().header("Authorization", admin)
            .multiPart("file", "brand.css", ":root { --color-brand-green: #123456; }".toByteArray(), "text/css")
            .`when`().put("/api/system/css").then().statusCode(204)

        given().`when`().get("/api/system/css")
            .then().statusCode(200)
            .contentType(containsString("text/css"))
            .header("Cache-Control", containsString("no-cache"))
            .body(containsString("--color-brand-green"))

        given().`when`().get("/api/system")
            .then().statusCode(200)
            .body("hasCustomCss", equalTo(true))
            .body("customCssFilename", equalTo("brand.css"))

        given().header("Authorization", admin)
            .`when`().delete("/api/system/css").then().statusCode(204)

        given().`when`().get("/api/system/css").then().statusCode(404)
    }

    @Test
    fun `an empty stylesheet is 400 and an oversize one is 400`() {
        given().header("Authorization", admin)
            .multiPart("file", "empty.css", "   ".toByteArray(), "text/css")
            .`when`().put("/api/system/css").then().statusCode(400)

        val oversize = "a".repeat(256 * 1024 + 1).toByteArray()
        given().header("Authorization", admin)
            .multiPart("file", "big.css", oversize, "text/css")
            .`when`().put("/api/system/css").then().statusCode(400)

        given().`when`().get("/api/system/css").then().statusCode(404)
    }

    @Test
    fun `standard effort drivers round-trip in list order`() {
        given().header("Authorization", admin)
            .contentType(ContentType.JSON)
            .body(
                """
                [
                  {"description":"Legacy integration","factor":0.15,"comment":"mainframe"},
                  {"description":"Regulatory audit","factor":0.1,"comment":""}
                ]
                """.trimIndent()
            )
            .`when`().put("/api/system/effort-drivers")
            .then().statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].description", equalTo("Legacy integration"))
            .body("[1].description", equalTo("Regulatory audit"))

        given().header("Authorization", estimator)
            .`when`().get("/api/system/effort-drivers")
            .then().statusCode(200)
            .body("[0].description", equalTo("Legacy integration"))
            .body("[0].factor", equalTo(0.15f))
            .body("[1].description", equalTo("Regulatory audit"))
    }

    @Test
    fun `a first draft is seeded with the standard drivers, a cloned draft is not`() {
        given().header("Authorization", admin)
            .contentType(ContentType.JSON)
            .body("""[{"description":"Standard driver","factor":0.2,"comment":"seeded"}]""")
            .`when`().put("/api/system/effort-drivers").then().statusCode(200)

        val projectId = given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"name":"System Settings Project"}""")
            .`when`().post("/api/projects").then().statusCode(201).extract().path<String>("id")
        val estimationId = given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"offer":"SYSSET-EST"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")

        // No submitted version to clone from → the template seeds the draft.
        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)
        given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("effortDrivers.size()", equalTo(1))
            .body("effortDrivers[0].description", equalTo("Standard driver"))

        // Submit, then create the next draft: it clones the submitted version's
        // drivers and must NOT get the template appended on top.
        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions/draft/submit")
            .then().statusCode(200)

        given().header("Authorization", admin)
            .contentType(ContentType.JSON)
            .body("""[{"description":"Changed template","factor":0.9,"comment":""}]""")
            .`when`().put("/api/system/effort-drivers").then().statusCode(200)

        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)
        given().header("Authorization", admin)
            .`when`().get("/api/estimations/$estimationId/versions/draft")
            .then().statusCode(200)
            .body("effortDrivers.size()", equalTo(1))
            .body("effortDrivers[0].description", equalTo("Standard driver"))
    }
}
