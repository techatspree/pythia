package io.pythia.ws

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletionStage

@QuarkusTest
class SessionSocketIT {

    private val moderator = "Dev dev-admin"

    private class Collector : WebSocket.Listener {
        val messages = CopyOnWriteArrayList<String>()
        private val buffer = StringBuilder()

        @Volatile
        var closed = false

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            buffer.append(data)
            if (last) {
                messages.add(buffer.toString())
                buffer.clear()
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            closed = true
            return null
        }
    }

    // Sets up project → estimation → draft (1 leaf) → session, returns the session id.
    private fun createRunnableSession(): String {
        val projectId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"name":"WS Project"}""")
            .post("/api/projects").then().statusCode(201).extract().path<String>("id")
        val estimationId = given().header("Authorization", moderator)
            .contentType(ContentType.JSON).body("""{"offer":"WS-EST"}""")
            .post("/api/projects/$projectId/estimations").then().statusCode(201).extract().path<String>("id")
        given().header("Authorization", moderator)
            .post("/api/estimations/$estimationId/versions").then().statusCode(201)
        given().header("Authorization", moderator).contentType(ContentType.JSON)
            .body("""{"roots":[{"type":"FIXED","description":"Leaf","minEffort":1.0,"expectedEffort":2.0,"maxEffort":3.0}]}""")
            .put("/api/estimations/$estimationId/versions/draft").then().statusCode(200)
        val leaf = given().header("Authorization", moderator)
            .get("/api/estimations/$estimationId/versions/draft").then().statusCode(200)
            .extract().path<String>("roots[0].logicalId")
        return given().header("Authorization", moderator).contentType(ContentType.JSON)
            .body("""{"estimationId":"$estimationId","title":"t","itemLogicalIds":["$leaf"]}""")
            .post("/api/sessions").then().statusCode(201).extract().path<String>("id")
    }

    private fun wsTicket(sessionId: String): String =
        given().header("Authorization", moderator)
            .post("/api/sessions/$sessionId/ws-ticket").then().statusCode(200).extract().path("ticket")

    private fun connect(sessionId: String, ticket: String, collector: Collector): WebSocket =
        HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(URI("ws://localhost:${RestAssured.port}/ws/sessions/$sessionId?ticket=$ticket"), collector)
            .get(5, TimeUnit.SECONDS)

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && !condition()) {
            Thread.sleep(50)
        }
    }

    @Test
    fun `a valid ticket connects and receives session snapshots on open and on a moderator action`() {
        val sessionId = createRunnableSession()
        val collector = Collector()
        connect(sessionId, wsTicket(sessionId), collector)

        // Snapshot pushed on open.
        await { collector.messages.any { it.contains("\"type\":\"session\"") } }
        assertTrue(collector.messages.any { it.contains("\"type\":\"session\"") }, "expected a session snapshot on open")

        // A moderator action broadcasts a fresh snapshot showing RUNNING.
        given().header("Authorization", moderator).post("/api/sessions/$sessionId/start").then().statusCode(200)
        await { collector.messages.any { it.contains("RUNNING") } }
        assertTrue(collector.messages.any { it.contains("RUNNING") }, "expected a broadcast after start")
    }

    // task-147: the client cannot tell a quiet room from a dead socket on its
    // own, so the server must keep feeding its watchdog. %test runs the schedule
    // at 1s so this does not sleep out the production 20s.
    @Test
    fun `an open connection keeps receiving heartbeat frames`() {
        val sessionId = createRunnableSession()
        val collector = Collector()
        connect(sessionId, wsTicket(sessionId), collector)

        await { collector.messages.any { it.contains("\"type\":\"heartbeat\"") } }
        assertTrue(
            collector.messages.any { it.contains("\"type\":\"heartbeat\"") },
            "expected a heartbeat frame on an open connection, got: ${collector.messages}"
        )

        // It is a recurring beat, not a one-off on connect — the watchdog is
        // reset by every frame, so a single heartbeat would not keep a quiet
        // room alive.
        val first = collector.messages.count { it.contains("\"type\":\"heartbeat\"") }
        await { collector.messages.count { it.contains("\"type\":\"heartbeat\"") } > first }
        assertTrue(
            collector.messages.count { it.contains("\"type\":\"heartbeat\"") } > first,
            "expected heartbeats to repeat, still $first after waiting"
        )
    }

    @Test
    fun `a bogus ticket is rejected — no session snapshot`() {
        val sessionId = createRunnableSession()
        val collector = Collector()
        connect(sessionId, "bogus-ticket", collector)

        await { collector.closed || collector.messages.any { it.contains("\"type\":\"error\"") } }
        assertFalse(
            collector.messages.any { it.contains("\"type\":\"session\"") },
            "a bogus ticket must not receive a session snapshot"
        )
        assertTrue(
            collector.closed || collector.messages.any { it.contains("\"type\":\"error\"") },
            "a bogus ticket must be rejected (error message and/or close)"
        )
    }
}
