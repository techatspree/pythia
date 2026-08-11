package io.pythia.auth.entra

import io.pythia.auth.Role
import jakarta.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntraRoleMapperTest {

    @Test
    fun `maps a JsonArray of JsonString - the real Entra token claim shape`() {
        // Reproduces the ClassCastException regression: read off the JWT, the
        // "roles" claim is a JsonArray whose elements are JsonString, not String.
        val claim = Json.createArrayBuilder().add("VIEWER").add("ADMIN").build()
        assertEquals(setOf(Role.VIEWER, Role.ADMIN), entraRolesToDomain(claim))
    }

    @Test
    fun `maps a single JsonString`() {
        assertEquals(setOf(Role.ESTIMATOR), entraRolesToDomain(Json.createValue("ESTIMATOR")))
    }

    @Test
    fun `maps a plain collection of String - the augmentor path`() {
        assertEquals(setOf(Role.VIEWER, Role.ESTIMATOR), entraRolesToDomain(setOf("VIEWER", "ESTIMATOR")))
    }

    @Test
    fun `matching is case-insensitive and unknown roles are ignored`() {
        val claim = Json.createArrayBuilder().add("viewer").add("SUPERUSER").build()
        assertEquals(setOf(Role.VIEWER), entraRolesToDomain(claim))
    }

    @Test
    fun `a null claim yields an empty set`() {
        assertEquals(emptySet<Role>(), entraRolesToDomain(null))
    }
}
