package io.pythia.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * The dev fixture's id source (task-212). Plain JUnit: [SeededIds] needs no
 * container, which is the point of keeping it a plain class.
 */
class SeededIdsTest {

    @Test
    fun `the same seed gives the same sequence`() {
        val a = SeededIds()
        val b = SeededIds()
        assertEquals(List(50) { a.next() }, List(50) { b.next() })
    }

    @Test
    fun `different seeds give different ids`() {
        assertNotEquals(SeededIds(1L).next(), SeededIds(2L).next())
    }

    @Test
    fun `every id is a well-formed name-based UUID`() {
        val ids = SeededIds()
        repeat(100) {
            val id = ids.next()
            assertEquals(3, id.version(), "version of $id")
            assertEquals(2, id.variant(), "variant of $id")
        }
    }

    @Test
    fun `ten thousand draws contain no duplicate`() {
        val ids = SeededIds()
        val drawn = List(10_000) { ids.next() }
        assertEquals(drawn.size, drawn.toSet().size)
    }

    @Test
    fun `drawn counts the calls`() {
        val ids = SeededIds()
        assertEquals(0, ids.drawn)
        repeat(7) { ids.next() }
        assertEquals(7, ids.drawn)
    }
}
