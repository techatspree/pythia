package io.pythia.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Every operation in the OpenAPI document states whether it needs a login
// (task-202). Role-gated operations get their requirement from @RolesAllowed and
// `/api/auth/me` from @Authenticated; the three public reads say so explicitly
// with `security: []` (an empty @SecurityRequirements). `[{}]` would mean
// "authentication optional", so the public ones are compared against exactly [].
@QuarkusTest
class OpenApiSecurityIT {

    private val httpMethods = setOf("get", "put", "post", "delete", "patch")

    private fun operations(): Map<String, Map<*, *>> {
        val paths: Map<String, Map<String, Any?>> = given()
            .`when`().get("/q/openapi?format=json")
            .then().statusCode(200)
            .extract().jsonPath().getMap("paths")
        return paths.flatMap { (path, item) ->
            item.filterKeys { it in httpMethods }.map { (verb, op) -> "${verb.uppercase()} $path" to op as Map<*, *> }
        }.toMap()
    }

    @Test
    fun `every operation declares its security`() {
        val ops = operations()
        assertTrue(ops.isNotEmpty())
        assertEquals(emptyList<String>(), ops.filterValues { it["security"] == null }.keys.toList())
    }

    @Test
    fun `the public reads declare no security`() {
        val ops = operations()
        listOf("GET /api/ping", "GET /api/system", "GET /api/system/css").forEach {
            assertEquals(emptyList<Any>(), ops.getValue(it)["security"], it)
        }
    }

    @Test
    fun `the current-user operations require a login but no role`() {
        val ops = operations()
        listOf("GET /api/auth/me", "PUT /api/auth/me/language").forEach {
            assertEquals(listOf(mapOf("SecurityScheme" to emptyList<String>())), ops.getValue(it)["security"], it)
        }
    }
}
