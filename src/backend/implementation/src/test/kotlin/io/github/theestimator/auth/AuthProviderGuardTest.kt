package io.github.theestimator.auth

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
}
