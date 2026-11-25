package io.pythia.mcp

import io.quarkiverse.mcp.server.test.McpAssured
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.vertx.core.MultiMap
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.UUID

// The read-only MCP server (task-218) end to end: the per-tenant switch, the
// authentication in front of it, the six tools on a real fixture, and the
// extension's DNS-rebinding check.
//
// The switch is GLOBAL (one system_settings row), so @AfterEach turns it off
// again. The %test profile seeds nothing (TestDataSeeder is dev-only), so the
// tool cases build their own fixture through the REST API.
@QuarkusTest
class McpServerIT {

    @TestHTTPResource("/")
    lateinit var root: URL

    private val admin = "Dev dev-admin"
    private val viewer = "Dev dev-viewer"

    private val initialize = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
          "protocolVersion":"2025-06-18","capabilities":{},
          "clientInfo":{"name":"mcp-server-it","version":"1"}}}
    """.trimIndent()

    @AfterEach
    fun switchOff() = setMcp(false)

    private fun setMcp(enabled: Boolean, auth: String = admin, expectedStatus: Int = 204) {
        given().header("Authorization", auth)
            .contentType(ContentType.JSON).body("""{"enabled": $enabled}""")
            .`when`().put("/api/system/mcp")
            .then().statusCode(expectedStatus)
    }

    private fun mcpRequest(auth: String? = viewer): RequestSpecification {
        val spec = given()
            .contentType(ContentType.JSON)
            .header("Accept", "application/json, text/event-stream")
        return if (auth == null) spec else spec.header("Authorization", auth)
    }

    private fun client(auth: String = viewer) = McpAssured.newStreamableClient()
        .setBaseUri(root.toURI())
        .setMcpPath("/api/mcp")
        .setAdditionalHeaders { MultiMap.caseInsensitiveMultiMap().add("Authorization", auth) }
        .build()
        .connect()

    // ── The switch and the authentication in front of it ────────────────────

    @Test
    fun `off by default the endpoint is absent for an authenticated caller`() {
        given().header("Authorization", viewer).`when`().get("/api/system")
            .then().statusCode(200).body("mcpEnabled", equalTo(false))

        mcpRequest().body(initialize).`when`().post("/api/mcp")
            .then().statusCode(404)
            .contentType(ContentType.JSON)
            .body("status", equalTo(404))
            .body("message", equalTo("The MCP interface is disabled for this installation"))
    }

    @Test
    fun `an anonymous caller gets 401 whether the switch is on or off`() {
        mcpRequest(auth = null).body(initialize).`when`().post("/api/mcp").then().statusCode(401)
        setMcp(true)
        mcpRequest(auth = null).body(initialize).`when`().post("/api/mcp").then().statusCode(401)
    }

    @Test
    fun `only an administrator may switch it`() {
        setMcp(true, auth = "Dev dev-estimator", expectedStatus = 403)
        setMcp(true)
        given().header("Authorization", viewer).`when`().get("/api/system")
            .then().statusCode(200).body("mcpEnabled", equalTo(true))
    }

    @Test
    fun `switched on a viewer sees exactly the six read tools`() {
        setMcp(true)
        client().use { mcp ->
            assertTrue(mcp.initResult().serverName().isNotBlank())
            mcp.`when`().toolsList { page ->
                assertEquals(
                    setOf("list_projects", "get_project", "get_estimation", "list_versions", "get_version",
                        "compare_versions"),
                    page.tools().map { it.name() }.toSet()
                )
            }.thenAssertResults()
        }
    }

    @Test
    fun `switching off again removes the endpoint at once`() {
        setMcp(true)
        mcpRequest().body(initialize).`when`().post("/api/mcp").then().statusCode(200)
        setMcp(false)
        mcpRequest().body("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""")
            .`when`().post("/api/mcp").then().statusCode(404)
    }

    // ── The tools, on a fixture built through the REST API ───────────────────

    private data class Fixture(val projectName: String, val projectId: String, val estimationId: String)

    // A project, an estimation, a draft imported from the Merlin sample,
    // submitted once (version 1), then a fresh draft: so "draft" and 1 exist.
    private fun fixture(): Fixture {
        val name = "MCP fixture ${UUID.randomUUID()}"
        val projectId = given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"name":"$name"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201).extract().path<String>("id")
        val estimationId = given().header("Authorization", admin)
            .contentType(ContentType.JSON).body("""{"offer":"MCP-${UUID.randomUUID()}"}""")
            .`when`().post("/api/projects/$projectId/estimations")
            .then().statusCode(201).extract().path<String>("id")
        val sample = javaClass.getResourceAsStream("/merlin/state.sql")!!.readBytes()
        given().header("Authorization", admin)
            .multiPart("file", "state.sql", sample, "application/octet-stream")
            .`when`().post("/api/estimations/$estimationId/versions/import/merlin")
            .then().statusCode(201)
        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions/draft/submit")
            .then().statusCode(200)
        given().header("Authorization", admin)
            .`when`().post("/api/estimations/$estimationId/versions")
            .then().statusCode(201)
        return Fixture(name, projectId, estimationId)
    }

    @Test
    fun `the tools return the same data the REST API does`() {
        val f = fixture()
        setMcp(true)
        client().use { mcp ->
            mcp.`when`()
                .toolsCall("list_projects") { r ->
                    assertFalse(r.isError)
                    assertTrue(r.content()[0].asText().text().contains(f.projectName))
                }
                .toolsCall("get_project", mapOf("projectId" to f.projectId)) { r ->
                    assertFalse(r.isError)
                    assertTrue(r.content()[0].asText().text().contains(f.estimationId))
                }
                .toolsCall("get_estimation", mapOf("estimationId" to f.estimationId)) { r ->
                    assertFalse(r.isError)
                    assertTrue(r.content()[0].asText().text().contains("\"hasDraft\":true"))
                }
                .toolsCall("list_versions", mapOf("estimationId" to f.estimationId)) { r ->
                    assertFalse(r.isError)
                    // The draft comes first.
                    assertTrue(r.content()[0].asText().text().startsWith("[{"))
                    assertTrue(r.content()[0].asText().text().indexOf("\"isDraft\":true") <
                        r.content()[0].asText().text().indexOf("\"isDraft\":false"))
                }
                .toolsCall("get_version", mapOf("estimationId" to f.estimationId, "version" to "draft")) { r ->
                    assertFalse(r.isError)
                    assertFalse(r.content()[0].asText().text().contains("\"totalEffort\":null"))
                }
                .toolsCall("get_version", mapOf("estimationId" to f.estimationId, "version" to "1")) { r ->
                    assertFalse(r.isError)
                    assertFalse(r.content()[0].asText().text().contains("\"totalEffort\":null"))
                }
                .toolsCall(
                    "compare_versions",
                    mapOf("estimationId" to f.estimationId, "versionA" to "1", "versionB" to "draft")
                ) { r ->
                    assertFalse(r.isError, r.content().toString())
                }
                .thenAssertResults()
        }
    }

    @Test
    fun `an unknown id is an error result, never data`() {
        setMcp(true)
        client().use { mcp ->
            mcp.`when`()
                .toolsCall("get_project", mapOf("projectId" to UUID.randomUUID().toString())) { r ->
                    assertTrue(r.isError)
                    assertTrue(r.content()[0].asText().text().contains("not found"))
                }
                .toolsCall(
                    "compare_versions",
                    mapOf("estimationId" to UUID.randomUUID().toString(), "versionA" to "1", "versionB" to "2")
                ) { r -> assertTrue(r.isError) }
                .thenAssertResults()
        }
    }

    // ── The extension's DNS-rebinding check (Origin, localhost-bound only) ───

    @Test
    fun `a request without Origin passes whatever Host the proxy forwards`() {
        setMcp(true)
        mcpRequest().header("Host", "pythia.example.org").body(initialize)
            .`when`().post("/api/mcp").then().statusCode(200)
    }

    @Test
    fun `a foreign Origin is refused while the server binds localhost`() {
        setMcp(true)
        mcpRequest().header("Origin", "https://pythia.example.org").body(initialize)
            .`when`().post("/api/mcp").then().statusCode(403)
    }
}
