package io.pythia.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Every operation in the OpenAPI document carries a unique, generated
// operationId (task-201). The ids come from the Kotlin method names
// (`operation-id-strategy=METHOD`) and a duplicate already fails augmentation
// (`duplicateOperationIdBehavior=FAIL`); this pins the served document itself,
// so a strategy that silently stopped applying is caught too.
@QuarkusTest
class OpenApiOperationIdIT {

    private val httpMethods = setOf("get", "put", "post", "delete", "patch")
    private val camelCase = Regex("^[a-z][A-Za-z0-9]*$")

    private fun operations(): List<Pair<String, String?>> {
        val paths: Map<String, Map<String, Any?>> = given()
            .`when`().get("/q/openapi?format=json")
            .then().statusCode(200)
            .extract().jsonPath().getMap("paths")
        return paths.flatMap { (path, item) ->
            item.filterKeys { it in httpMethods }.map { (verb, op) ->
                "${verb.uppercase()} $path" to (op as Map<*, *>)["operationId"] as String?
            }
        }
    }

    @Test
    fun `every operation has an operationId`() {
        val missing = operations().filter { it.second == null }.map { it.first }
        assertTrue(operations().isNotEmpty())
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `operationIds are unique`() {
        val duplicates = operations().mapNotNull { it.second }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun `operationIds are camelCase method names without a class prefix`() {
        val malformed = operations().mapNotNull { it.second }.filterNot { camelCase.matches(it) }
        assertEquals(emptyList<String>(), malformed)
    }

    @Test
    fun `known operations carry their expected ids`() {
        val ids = operations().mapNotNull { it.second }.toSet()
        listOf("getProject", "listProjects", "changeOwner", "getSession", "getSystemSettings", "ping", "adminPing")
            .forEach { assertTrue(it in ids, "missing operationId $it") }
    }
}
