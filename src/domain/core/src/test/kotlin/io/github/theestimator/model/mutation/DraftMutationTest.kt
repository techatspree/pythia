package io.github.theestimator.model.mutation

import io.github.theestimator.model.EstimationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DraftMutationTest {

    private fun version(notes: String = "", num: Int = 1) =
        EstimationVersion(versionNumber = num, notes = notes)

    @Test
    fun `apply of ReplaceWholeDraft returns its after`() {
        val before = version(notes = "before")
        val after = version(notes = "after")
        val mutation = ReplaceWholeDraft(before, after)
        assertEquals(after, mutation.apply(before))
    }

    @Test
    fun `inverse applied to after yields before`() {
        val before = version(notes = "before")
        val after = version(notes = "after")
        val mutation = ReplaceWholeDraft(before, after)
        assertEquals(before, mutation.inverse().apply(after))
    }

    @Test
    fun `inverse of inverse equals the original mutation`() {
        val mutation = ReplaceWholeDraft(version(notes = "a"), version(notes = "b"))
        assertEquals(mutation, mutation.inverse().inverse())
    }

    @Test
    fun `diff of structurally equal versions returns null`() {
        assertNull(version().diff(version()))
    }

    @Test
    fun `diff of differing versions returns a reversible ReplaceWholeDraft`() {
        val before = version(notes = "before")
        val after = version(notes = "after")
        val mutation = before.diff(after)
        assertNotNull(mutation)
        assertTrue(mutation is ReplaceWholeDraft)
        assertEquals(before, mutation!!.inverse().apply(after))
    }
}
