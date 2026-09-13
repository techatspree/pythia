package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.StandardMethods
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.UUID
import java.io.ByteArrayOutputStream
import io.pythia.method.EstimationMethod

class ExcelImporterTreeTest {

    // The exporters resolve the method module from EstimationMethodRegistry,
    // which no longer self-populates (task-143). These are plain JUnit tests, so
    // the backend's StartupEvent bootstrap never fires — install explicitly.
    @BeforeEach
    fun installMethods() = StandardMethods.installAll()

    private val exporter = ExcelExporter()
    private val importer = ExcelImporter()

    @Test
    fun `export then re-import preserves tree shape AND logicalIds`() {
        val source = TreeFixtures.threeLevel()

        val xlsx = ByteArrayOutputStream()
        exporter.export(source, EstimationMethod.THREE_POINT_PERT, emptyList(), xlsx)

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
        val srcAuth = (srcBackend as io.pythia.domain.submitted.SubmittedGroupNode).children[0]
        val srcHealth = srcBackend.children[1]
        val srcToken = (srcAuth as io.pythia.domain.submitted.SubmittedGroupNode).children[0]
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

    // task-182: a bucket+sampled workbook must come back as bucket items. Before
    // it, the importer read fixed columns positionally and built DraftFixedItemNode
    // for every leaf, so the bucket, the sample flag and the five-column layout
    // were all lost on the way in.
    @Test
    fun `export then re-import preserves buckets, sample flags and bucket order`() {
        val bucketA = EstimationBucket().apply { id = UUID.randomUUID(); label = "Frontend"; position = 0 }
        val bucketB = EstimationBucket().apply { id = UUID.randomUUID(); label = "Backend"; position = 1 }
        val source = TreeFixtures.bucketed(bucketA, bucketB)

        val xlsx = ByteArrayOutputStream()
        exporter.export(source, EstimationMethod.BUCKET_SAMPLED_PERT, listOf(bucketA, bucketB), xlsx)

        // Import into an estimation that has NO buckets: they must be recreated
        // from the sheet, matched by label rather than by the file's ids.
        val target = Estimation().apply { method = EstimationMethod.BUCKET_SAMPLED_PERT }
        val draft = importer.import(ByteArrayInputStream(xlsx.toByteArray()), target, versionNumber = 1)

        assertEquals(2, target.buckets.size, "both buckets should have been created on the target")
        val frontend = target.buckets.first { it.label == "Frontend" }
        val backend = target.buckets.first { it.label == "Backend" }
        assertEquals(0, frontend.position, "bucket order must survive the round trip")
        assertEquals(1, backend.position)

        val group = draft.roots.single()
        assertTrue(group is DraftGroupNode)
        assertEquals(2, (group as DraftGroupNode).children.size)

        val sample = group.children[0]
        assertTrue(sample is DraftBucketedItemNode, "a bucketed leaf must not come back as a fixed item")
        sample as DraftBucketedItemNode
        assertEquals("Sample A", sample.description)
        assertEquals(true, sample.isSample)
        assertEquals(frontend, sample.bucket, "the leaf must point at THIS estimation's bucket")
        assertEquals(1.0, sample.minEffort)
        assertEquals(2.0, sample.expectedEffort)
        assertEquals(3.0, sample.maxEffort)

        val nonSample = group.children[1]
        assertTrue(nonSample is DraftBucketedItemNode)
        nonSample as DraftBucketedItemNode
        assertEquals(false, nonSample.isSample)
        assertEquals(backend, nonSample.bucket)
        // A non-sample's numbers come from its bucket's samples; the round trip
        // must not fabricate a triple for it.
        assertNull(nonSample.minEffort)
        assertNull(nonSample.expectedEffort)
    }
}
