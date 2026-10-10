package io.pythia.auth

import io.quarkus.runtime.LaunchMode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthProviderGuardTest {

    @Test
    fun `dev provider under a NORMAL launch is not allowed`() {
        assertFalse(AuthProviderGuard.devProviderAllowed("dev", LaunchMode.NORMAL))
    }

    @Test
    fun `dev provider under DEVELOPMENT is allowed`() {
        assertTrue(AuthProviderGuard.devProviderAllowed("dev", LaunchMode.DEVELOPMENT))
    }

    @Test
    fun `dev provider under TEST is allowed`() {
        assertTrue(AuthProviderGuard.devProviderAllowed("dev", LaunchMode.TEST))
    }

    @Test
    fun `entra provider under a NORMAL launch is allowed`() {
        assertTrue(AuthProviderGuard.devProviderAllowed("entra", LaunchMode.NORMAL))
    }

    @Test
    fun `configured provider whose module is in the build is available`() {
        assertTrue(AuthProviderGuard.moduleAvailable("entra", listOf("entra")))
    }

    @Test
    fun `a build with no auth module at all is not available`() {
        assertFalse(AuthProviderGuard.moduleAvailable("entra", emptyList()))
    }

    @Test
    fun `dev against a production build is not available`() {
        assertFalse(AuthProviderGuard.moduleAvailable("dev", listOf("entra")))
    }

    @Test
    fun `a provider no module implements is not available`() {
        assertFalse(AuthProviderGuard.moduleAvailable("keycloak", listOf("entra")))
    }
}
