package io.pythia.rest

import io.pythia.domain.Project
import io.pythia.domain.User
import io.pythia.repository.UserRepository
import io.pythia.service.ProjectService
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

// Project ownership (task-200): the creator owns a project, `owner=me` filters
// to the caller's own projects without provisioning anybody, and only the owner
// or an ADMIN may transfer ownership.
@QuarkusTest
class ProjectOwnershipIT {

    @Inject
    lateinit var userRepository: UserRepository

    private fun createProject(asUser: String, name: String): String =
        given()
            .header("Authorization", "Dev $asUser")
            .contentType(ContentType.JSON)
            .body("""{"name":"$name","client":"Nordwind Logistik GmbH"}""")
            .`when`().post("/api/projects")
            .then().statusCode(201)
            .extract().path("id")

    private fun ownerIdOf(asUser: String, projectId: String): String? =
        given().header("Authorization", "Dev $asUser")
            .`when`().get("/api/projects/$projectId")
            .then().statusCode(200)
            .extract().path("owner.id")

    private fun userIdByName(displayName: String): String =
        given().header("Authorization", "Dev dev-admin")
            .`when`().get("/api/users")
            .then().statusCode(200)
            .extract().jsonPath().getList<Map<String, Any>>("")
            .first { it["displayName"] == displayName }["id"] as String

    private fun unique(base: String) = "$base ${UUID.randomUUID().toString().take(8)}"

    @Test
    fun `the creator owns a new project and may change its owner`() {
        val id = createProject("dev-estimator", unique("Kundenportal"))
        given().header("Authorization", "Dev dev-estimator")
            .`when`().get("/api/projects/$id")
            .then().statusCode(200)
            .body("owner.displayName", equalTo("Dev Estimator"))
            .body("canChangeOwner", equalTo(true))
    }

    @Test
    fun `owner=me lists only the caller's own projects`() {
        val mine = createProject("dev-estimator", unique("Lagerverwaltung"))
        val theirs = createProject("dev-admin", unique("Zeiterfassung"))

        val ids: List<String> = given().header("Authorization", "Dev dev-estimator")
            .queryParam("owner", "me")
            .`when`().get("/api/projects")
            .then().statusCode(200)
            .extract().path("id")
        assertTrue(mine in ids, "own project must be listed")
        assertFalse(theirs in ids, "another user's project must not be listed")

        // Without the filter, everything is visible.
        given().header("Authorization", "Dev dev-estimator")
            .`when`().get("/api/projects")
            .then().statusCode(200)
            .body("id", hasItem(theirs))
    }

    @Test
    fun `an unsupported owner filter is rejected`() {
        given().header("Authorization", "Dev dev-estimator")
            .queryParam("owner", "someone-else")
            .`when`().get("/api/projects")
            .then().statusCode(400)
    }

    @Test
    fun `listing my projects never provisions a user`() {
        // dev-viewer owns nothing (a viewer cannot create projects). Whether or
        // not another test already provisioned it, a READ must not add a row.
        val before = userRepository.count()
        given().header("Authorization", "Dev dev-viewer")
            .queryParam("owner", "me")
            .`when`().get("/api/projects")
            .then().statusCode(200)
            .body("size()", equalTo(0))
        assertEquals(before, userRepository.count(), "a read must not provision a user")
    }

    @Test
    fun `a user without a row may not change any owner`() {
        // The read-only resolution yields null for an unprovisioned caller; the
        // shared rule must then deny unless the caller is an admin.
        val project = Project().apply { owner = User() }
        assertFalse(ProjectService.mayChangeOwner(project, null, userIsAdmin = false))
        assertTrue(ProjectService.mayChangeOwner(project, null, userIsAdmin = true))
    }

    @Test
    fun `the owner transfers the project and then loses the right to change it`() {
        val id = createProject("dev-estimator", unique("Reisekostenportal"))
        val adminId = userIdByName("Dev Admin")

        given().header("Authorization", "Dev dev-estimator")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"$adminId"}""")
            .`when`().put("/api/projects/$id/owner")
            .then().statusCode(200)
            .body("owner.id", equalTo(adminId))
            .body("canChangeOwner", equalTo(false))

        assertEquals(adminId, ownerIdOf("dev-estimator", id))
    }

    @Test
    fun `an estimator who is not the owner is refused with a JSON 403`() {
        val id = createProject("dev-admin", unique("Vertragsarchiv"))
        val estimatorId = userIdByName("Dev Estimator")

        given().header("Authorization", "Dev dev-estimator")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"$estimatorId"}""")
            .`when`().put("/api/projects/$id/owner")
            .then().statusCode(403)
            .contentType(ContentType.JSON)
            .body("message", not(blankOrNullString()))
            .body("status", equalTo(403))

        given().header("Authorization", "Dev dev-estimator")
            .`when`().get("/api/projects/$id")
            .then().statusCode(200)
            .body("canChangeOwner", equalTo(false))
    }

    @Test
    fun `a viewer is stopped by the role gate`() {
        val id = createProject("dev-estimator", unique("Schulungsplattform"))
        given().header("Authorization", "Dev dev-viewer")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"${UUID.randomUUID()}"}""")
            .`when`().put("/api/projects/$id/owner")
            .then().statusCode(403)
    }

    @Test
    fun `an admin may change the owner of a project it does not own`() {
        val id = createProject("dev-estimator", unique("Bewerbermanagement"))
        val adminId = userIdByName("Dev Admin")

        given().header("Authorization", "Dev dev-admin")
            .`when`().get("/api/projects/$id")
            .then().statusCode(200)
            .body("canChangeOwner", equalTo(true))

        given().header("Authorization", "Dev dev-admin")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"$adminId"}""")
            .`when`().put("/api/projects/$id/owner")
            .then().statusCode(200)
            .body("owner.id", equalTo(adminId))
    }

    @Test
    fun `an unknown new owner is 400 and an unknown project is 404`() {
        val id = createProject("dev-estimator", unique("Ticketsystem"))
        given().header("Authorization", "Dev dev-estimator")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"${UUID.randomUUID()}"}""")
            .`when`().put("/api/projects/$id/owner")
            .then().statusCode(400)

        given().header("Authorization", "Dev dev-admin")
            .contentType(ContentType.JSON)
            .body("""{"ownerId":"${UUID.randomUUID()}"}""")
            .`when`().put("/api/projects/${UUID.randomUUID()}/owner")
            .then().statusCode(404)
    }

    @Test
    fun `the user list is sorted by display name`() {
        createProject("dev-estimator", unique("Sortierprobe"))
        createProject("dev-admin", unique("Sortierprobe"))
        val names: List<String?> = given().header("Authorization", "Dev dev-viewer")
            .`when`().get("/api/users")
            .then().statusCode(200)
            .body("displayName", not(hasItem(nullValue())))
            .extract().path("displayName")
        assertEquals(names.sortedBy { it }, names)
        assertTrue("Dev Admin" in names && "Dev Estimator" in names)
    }
}
