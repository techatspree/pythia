package io.pythia.method.bucketsampled

import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodRegistry
import io.pythia.model.CalculationParameters
import io.pythia.model.EstimationGroup
import io.pythia.model.EstimationItem
import io.pythia.model.EstimationVersion
import io.pythia.model.leaves
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BucketMethodModuleTest {

    private val module = BucketMethodModule()

    @BeforeEach
    fun ensureRegistryPopulated() {
        // The registry is a process-wide singleton shared with every other test
        // in this module's JVM that calls EstimationVersion.calculate(), and it
        // no longer self-populates (task-143). Registering only adds/overwrites,
        // never removes, so it is safe under Gradle's parallel test execution —
        // unlike clear(), which would empty it mid-read and race the others.
        // Only this module's own method is registered: the standard installer
        // lives in the aggregator :domain, which a method module cannot see.
        EstimationMethodRegistry.register(BucketMethodModule())
    }

    private fun sample(bucket: String, o: Double, l: Double, p: Double) =
        BucketedEstimationItem(bucketId = bucket, isSample = true, optimistic = o, likely = l, pessimistic = p)

    private fun nonSample(bucket: String) = BucketedEstimationItem(bucketId = bucket, isSample = false)

    @Test
    fun `calculateAll assigns each bucket average to its non-sample leaves`() {
        val items = listOf(
            sample("b1", 1.0, 2.0, 3.0),   // mean 2.0
            sample("b1", 3.0, 6.0, 9.0),   // mean 6.0
            sample("b2", 2.0, 4.0, 6.0),   // mean 4.0
            sample("b2", 4.0, 8.0, 12.0),  // mean 8.0
            nonSample("b1"),
            nonSample("b1"),
            nonSample("b2")
        )

        val result = module.calculateAll(items, CalculationParameters())
            .filterIsInstance<BucketedEstimationItem>()

        // Samples keep their own PERT mean.
        assertEquals(2.0, result[0].mean, 1e-9)
        assertEquals(6.0, result[1].mean, 1e-9)
        assertEquals(4.0, result[2].mean, 1e-9)
        assertEquals(8.0, result[3].mean, 1e-9)
        // Non-samples inherit their bucket's average: b1 -> 4.0, b2 -> 6.0.
        assertEquals(4.0, result[4].mean, 1e-9)
        assertEquals(4.0, result[5].mean, 1e-9)
        assertEquals(6.0, result[6].mean, 1e-9)
    }

    @Test
    fun `export column headers and row cells have matching arity`() {
        val item = sample("b1", 1.0, 2.0, 3.0)
        assertEquals(module.exportColumnHeaders().size, module.exportRow(item).size)
        assertEquals(module.exportColumnHeaders().size, module.exportRow(nonSample("b1")).size)
    }

    /**
     * A leaf the bucket method does not own. The real one is PERT's
     * TimeRelativeEstimationItem, which this module cannot see (task-143), so a
     * minimal stand-in carries just the triple.
     */
    private class ForeignItem(min: Double, exp: Double, max: Double) :
        EstimationItem(description = "foreign", minEffort = min, expectedEffort = exp, maxEffort = max) {
        override fun withCalculationParameters(params: CalculationParameters): EstimationItem = this
        override val method: EstimationMethod = EstimationMethod.THREE_POINT_PERT
        override val nodeTypeLabel: String = "TIME_RELATIVE"
        override val isScheduled: Boolean = false
    }

    @Test
    fun `a leaf the method does not own exports its triple in the sample columns`() {
        // task-187: this used to be five empty cells, so accompanying work in a
        // bucket estimation lost its effort on export.
        assertEquals(listOf("", "", "0.5", "1.0", "2.0"), module.exportRow(ForeignItem(0.5, 1.0, 2.0)))
    }

    @Test
    fun `effortColumnOffsets point at a sample row's triple`() {
        val row = module.exportRow(sample("b1", 1.0, 2.0, 3.0))
        assertEquals(listOf("1.0", "2.0", "3.0"), module.effortColumnOffsets.map { row[it] })
        val headers = module.exportColumnHeaders()
        assertEquals(listOf("Optimistic", "Likely", "Pessimistic"), module.effortColumnOffsets.map { headers[it] })
    }

    @Test
    fun `non-sample export row has empty three-point cells`() {
        assertEquals(listOf("b1", "false", "", "", ""), module.exportRow(nonSample("b1")))
        assertEquals(listOf("b1", "true", "1.0", "2.0", "3.0"), module.exportRow(sample("b1", 1.0, 2.0, 3.0)))
    }

    @Test
    fun `the module is resolvable from the registry once registered`() {
        assertNotNull(EstimationMethodRegistry.get(EstimationMethod.BUCKET_SAMPLED_PERT))
    }

    @Test
    fun `a bucketed tree computes end-to-end via EstimationVersion calculate`() {
        val version = EstimationVersion(
            versionNumber = 1,
            stdDevFactor = 2.0,
            dailyRate = 800.0,
            salesSurcharge = 0.1,
            roots = listOf(
                EstimationGroup(
                    title = "Bucket b1",
                    children = listOf(
                        sample("b1", 1.0, 2.0, 3.0),   // mean 2.0
                        sample("b1", 3.0, 6.0, 9.0),   // mean 6.0
                        nonSample("b1")                // inherits (2+6)/2 = 4.0
                    )
                )
            )
        )

        val calculated = version.calculate()
        val calcLeaves = calculated.roots.flatMap { it.leaves().toList() }.filterIsInstance<BucketedEstimationItem>()

        val resolvedNonSample = calcLeaves.single { !it.isSample }
        assertEquals(4.0, resolvedNonSample.mean, 1e-9)

        // Group accumulation and version total are consistent with the leaves.
        val group = calculated.roots.single() as EstimationGroup
        assertEquals(group.children.sumOf { it.offerPT }, group.offerPT, 1e-9)
        assertEquals(calculated.roots.sumOf { it.offerPT }, calculated.totalEffort, 1e-9)
    }
}
