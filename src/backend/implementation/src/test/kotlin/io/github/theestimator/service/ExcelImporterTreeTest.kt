package io.github.theestimator.service

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExcelImporterTreeTest {

    private val exporter = ExcelExporter()
    private val importer = ExcelImporter()

    @Test
    fun `export then re-import preserves tree shape AND logicalIds`() {
        val source = TreeFixtures.threeLevel()

        val xlsx = ByteArrayOutputStream()
        exporter.export(source, xlsx)

        val draft = importer.import(ByteArrayInputStream(xlsx.toByteArray()), Estimation(), versionNumber = 1)

        // Shape: 1 root group with 2 children — inner group + leaf.
        assertEquals(1, draft.roots.size)
        val backend = draft.roots.single()
        assertTrue(backend is DraftGroupNode)
        assertEquals("Backend", (backend as DraftGroupNode).title)
        assertEquals(2, backend.children.size)

        val auth = backend.children[0]
        assertTrue(auth is DraftGroupNode)
        assertEquals("Auth", (auth as DraftGroupNode).title)
        assertEquals(2, auth.children.size)

        val token = auth.children[0]
        val session = auth.children[1]
        assertTrue(token is DraftFixedItemNode)
        assertTrue(session is DraftTimeRelativeItemNode)
        assertEquals("Token endpoint", token.description)
        assertEquals("Session storage", session.description)
        assertEquals("h/Woche", (session as DraftTimeRelativeItemNode).unit)

        val health = backend.children[1]
        assertTrue(health is DraftFixedItemNode)
        assertEquals("Health endpoint", health.description)

        // Positions are tree-relative and zero-based.
        assertEquals(0, backend.position)
        assertEquals(0, auth.position)
        assertEquals(1, health.position)
        assertEquals(0, token.position)
        assertEquals(1, session.position)

        // The critical property task-052's diff relies on: logicalIds survive
        // the round-trip — they don't get regenerated on import.
        val srcBackend = source.roots.single()
        val srcAuth = (srcBackend as io.github.theestimator.domain.submitted.SubmittedGroupNode).children[0]
        val srcHealth = srcBackend.children[1]
        val srcToken = (srcAuth as io.github.theestimator.domain.submitted.SubmittedGroupNode).children[0]
        val srcSession = srcAuth.children[1]

        assertEquals(srcBackend.logicalId, backend.logicalId, "Backend logicalId must round-trip")
        assertEquals(srcAuth.logicalId, auth.logicalId, "Auth logicalId must round-trip")
        assertEquals(srcToken.logicalId, token.logicalId, "Token leaf logicalId must round-trip")
        assertEquals(srcSession.logicalId, session.logicalId, "Session leaf logicalId must round-trip")
        assertEquals(srcHealth.logicalId, health.logicalId, "Health leaf logicalId must round-trip")

        // Parent linkage is correctly wired.
        assertNotNull(auth.parent)
        assertEquals(backend.logicalId, (auth.parent as DraftEstimationNode).logicalId)
        assertEquals(auth.logicalId, (token.parent as DraftEstimationNode).logicalId)
    }
}
