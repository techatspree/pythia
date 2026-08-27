package io.pythia.auth

import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.SmallRyeConfigBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Properties

/**
 * Regression guard for the profile -> auth-module mapping. Resolves
 * `app.auth.provider` directly against the real `application.properties` via
 * SmallRye Config (no Quarkus boot, no Docker), so it fails the moment a
 * profile stops selecting its intended module — most importantly that
 * `dev-minikube` keeps selecting the Entra module.
 *
 * `prod` is the exception and is asserted the other way round (task-161): it
 * must resolve to NOTHING here, because a deployed stage's provider is a stage
 * coordinate that arrives as APP_AUTH_PROVIDER from the Kubernetes ConfigMap.
 * Baking one back into the image is what this guard now catches.
 */
class AuthProviderProfileTest {

    private fun resolveProvider(profile: String): String {
        val props = Properties()
        requireNotNull(
            Thread.currentThread().contextClassLoader
                .getResourceAsStream("application.properties")
        ) { "application.properties not found on the test classpath" }
            .use { props.load(it) }

        val map = props.entries.associate { it.key.toString() to it.value.toString() }
        val config = SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .withProfile(profile)
            .withSources(PropertiesConfigSource(map, "application.properties", 100))
            .build()
        return config.getValue("app.auth.provider", String::class.java)
    }

    @Test
    fun `dev-minikube profile resolves auth provider to entra`() {
        assertEquals("entra", resolveProvider("dev-minikube"))
    }

    @Test
    fun `prod profile bakes NO auth provider — it comes from the ConfigMap`() {
        // Fail-closed by construction: with no value in the image, a prod launch
        // that is not given APP_AUTH_PROVIDER cannot start at all, rather than
        // falling back to the forgeable dev module.
        assertThrows(NoSuchElementException::class.java) { resolveProvider("prod") }
    }

    @Test
    fun `dev profile resolves auth provider to dev`() {
        assertEquals("dev", resolveProvider("dev"))
    }

    @Test
    fun `test profile resolves auth provider to dev`() {
        assertEquals("dev", resolveProvider("test"))
    }
}
