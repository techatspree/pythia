package io.pythia.auth

import io.pythia.repository.UserRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class UserProvisioningIT {

    @Inject
    lateinit var userRepository: UserRepository

    @Test
    @Transactional
    fun `an authenticated mutating request provisions the current user`() {
        // Sanity: a mutating request as dev-admin (the DevAdminAuth header).
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Provisioning Test"}""")
            .`when`()
            .post("/api/projects")
            .then()
            .statusCode(201)

        // The just-in-time provisioning filter must have created a User row
        // for the dev-admin principal.
        val user = userRepository.findByEntraSubjectId("dev-admin")
        assertNotNull(user, "dev-admin should be provisioned into the users table after a mutating request")
        assertEquals("Dev Admin", user!!.displayName)
    }
}
