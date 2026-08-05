package io.github.theestimator.method.bucketsampled

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.model.CalculationParameters
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.leaves
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BucketMethodModuleTest {

    private val module = BucketMethodModule()

    @BeforeEach
    fun ensureRegistryPopulated() {
        // The registry is a process-wide singleton shared with every other test
        // that calls EstimationVersion.calculate(). installStandardMethods only
        // adds/overwrites (never removes), so it is safe under Gradle's parallel
        // test execution — unlike clear(), which would empty it mid-read and race
        // the other classes. It self-populates via init{}; this just guarantees it.
        EstimationMethodRegistry.installStandardMethods()
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

    @Test
    fun `non-sample export row has empty three-point cells`() {
        assertEquals(listOf("b1", "false", "", "", ""), module.exportRow(nonSample("b1")))
        assertEquals(listOf("b1", "true", "1.0", "2.0", "3.0"), module.exportRow(sample("b1", 1.0, 2.0, 3.0)))
    }

    @Test
    fun `the module is registered after installStandardMethods`() {
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
