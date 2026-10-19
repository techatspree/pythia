package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.StandardMethods
import io.pythia.domain.submitted.SubmittedProjectPhase
import io.pythia.domain.submitted.SubmittedTimeRelativeItemNode
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

    // This is also the round-trip guard for task-185's styling: the exporter now
    // writes depth as CellStyle.indention instead of leading spaces, and the exact
    // titles/descriptions asserted at each depth below fail on a stray space or a
    // lost level. ExcelImporter keeps its trimStart() regardless — workbooks
    // exported before task-185 still carry the spaces.
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

    // task-187: accompanying work in a bucket workbook. Before, the bucket export
    // wrote five empty cells for it and the import read the PERT columns 1..3,
    // which in a bucket sheet hold Bucket / Is Sample / Optimistic — so its
    // effort was lost both ways.
    @Test
    fun `accompanying work survives a bucket workbook round trip`() {
        val bucketA = EstimationBucket().apply { id = UUID.randomUUID(); label = "Frontend"; position = 0 }
        val bucketB = EstimationBucket().apply { id = UUID.randomUUID(); label = "Backend"; position = 1 }
        val source = TreeFixtures.bucketed(bucketA, bucketB)
        val umsetzung = SubmittedProjectPhase().apply {
            name = "Umsetzung"; abbreviation = "UM"; durationWeeks = 12.0; version = source
        }
        source.phases.add(umsetzung)
        val accompanying = SubmittedTimeRelativeItemNode().apply {
            logicalId = UUID.randomUUID()
            description = "Projektbegleitung"
            unit = "h/Woche"
            minEffort = 0.5; expectedEffort = 1.0; maxEffort = 2.0
            phaseAbbreviation = "UM"
            mean = 13.0; variance = 0.3; offerPT = 13.0
            position = 1
            version = source
        }
        source.roots.add(accompanying)

        val xlsx = ByteArrayOutputStream()
        exporter.export(source, EstimationMethod.BUCKET_SAMPLED_PERT, listOf(bucketA, bucketB), xlsx)

        val target = Estimation().apply { method = EstimationMethod.BUCKET_SAMPLED_PERT }
        val draft = importer.import(ByteArrayInputStream(xlsx.toByteArray()), target, versionNumber = 1)

        assertEquals(2, draft.roots.size)
        val imported = draft.roots[1]
        assertTrue(imported is DraftTimeRelativeItemNode, "accompanying work must stay TIME_RELATIVE")
        imported as DraftTimeRelativeItemNode
        assertEquals("Projektbegleitung", imported.description)
        assertEquals("h/Woche", imported.unit)
        assertEquals(0.5, imported.minEffort)
        assertEquals(1.0, imported.expectedEffort)
        assertEquals(2.0, imported.maxEffort)
        assertEquals("UM", imported.phase?.abbreviation)
        assertEquals(accompanying.logicalId, imported.logicalId)

        // The bucketed leaves beside it are untouched by the new column reading.
        val sample = (draft.roots[0] as DraftGroupNode).children[0] as DraftBucketedItemNode
        assertEquals(1.0, sample.minEffort)
        assertEquals(3.0, sample.maxEffort)
    }

    // task-213: the "Parameter" sheet records the hours per working day the
    // version was calculated with, and leaves the row out for a version
    // submitted before the conversion (NULL). The importer ignores the row:
    // an imported draft follows the tenant setting like every other draft.
    @Test
    fun `the parameter sheet records hours per day, omits a legacy null, and import ignores it`() {
        fun parameterRows(source: io.pythia.domain.submitted.SubmittedEstimationVersion): Map<String, Double> {
            val xlsx = ByteArrayOutputStream()
            exporter.export(source, EstimationMethod.THREE_POINT_PERT, emptyList(), xlsx)
            val sheet = org.apache.poi.xssf.usermodel.XSSFWorkbook(ByteArrayInputStream(xlsx.toByteArray()))
                .getSheet(ExcelGermanLabels.Sheets.PARAMETERS)
            return (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
                .associate { it.getCell(0).stringCellValue to it.getCell(1).numericCellValue }
        }

        val legacy = TreeFixtures.threeLevel()
        assertNull(legacy.hoursPerDay)
        assertTrue(ExcelGermanLabels.Parameters.HOURS_PER_DAY !in parameterRows(legacy))

        val recorded = TreeFixtures.threeLevel().apply { hoursPerDay = 7.5 }
        assertEquals(7.5, parameterRows(recorded)[ExcelGermanLabels.Parameters.HOURS_PER_DAY])

        val xlsx = ByteArrayOutputStream()
        exporter.export(recorded, EstimationMethod.THREE_POINT_PERT, emptyList(), xlsx)
        val draft = importer.import(ByteArrayInputStream(xlsx.toByteArray()), Estimation(), versionNumber = 1)
        // The three real parameters still import; the hours row changes nothing.
        assertEquals(recorded.dailyRate, draft.dailyRate)
        assertEquals(1, draft.roots.size)
    }
}
