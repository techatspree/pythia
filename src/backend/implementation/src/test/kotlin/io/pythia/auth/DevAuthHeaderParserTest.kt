package io.pythia.auth

import io.pythia.auth.dev.DevAuthResult
import io.pythia.auth.dev.resolveDevUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * Locks in the strict-only contract of the dev auth header parser: with no
 * default-user fallback, a missing/blank header is Anonymous (→ 401), and any
 * malformed/unknown header is Reject (→ 401).
 */
class DevAuthHeaderParserTest {

    @Test
    fun `null header is Anonymous`() {
        assertInstanceOf(DevAuthResult.Anonymous::class.java, resolveDevUser(null))
    }

    @Test
    fun `blank header is Anonymous`() {
        assertInstanceOf(DevAuthResult.Anonymous::class.java, resolveDevUser("   "))
    }

    @Test
    fun `valid Dev header authenticates the known subject`() {
        val result = resolveDevUser("Dev dev-admin")
        assertInstanceOf(DevAuthResult.Authenticated::class.java, result)
        assertEquals("dev-admin", (result as DevAuthResult.Authenticated).user.subjectId)
    }

    @Test
    fun `unknown subject is Reject`() {
        assertInstanceOf(DevAuthResult.Reject::class.java, resolveDevUser("Dev nope-not-a-user"))
    }

    @Test
    fun `Bearer scheme is Reject`() {
        assertInstanceOf(DevAuthResult.Reject::class.java, resolveDevUser("Bearer x.y.z"))
    }

    @Test
    fun `lowercase dev scheme is Reject`() {
        assertInstanceOf(DevAuthResult.Reject::class.java, resolveDevUser("dev dev-admin"))
    }
}
