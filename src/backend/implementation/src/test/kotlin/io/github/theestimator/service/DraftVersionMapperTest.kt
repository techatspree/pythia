package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DraftVersionMapperTest {

    private val mapper = DraftVersionMapper()

    private fun fixedLeaf(version: DraftEstimationVersion, desc: String, min: Double, exp: Double, max: Double) =
        DraftFixedItemNode().apply {
            this.version = version
            this.description = desc
            this.minEffort = min
            this.expectedEffort = exp
            this.maxEffort = max
        }

    @Test
    fun `three-level entity tree maps to a three-level domain tree`() {
        val draft = DraftEstimationVersion().apply { versionNumber = 1 }

        val deepLeaf = fixedLeaf(draft, "deep", 1.0, 2.0, 3.0)
        val midLeaf  = fixedLeaf(draft, "mid",  2.0, 4.0, 6.0)

        val innerGroup = DraftGroupNode().apply {
            this.version = draft
            this.title = "inner"
            children.add(deepLeaf.also { it.parent = this; it.position = 0 })
        }

        val rootGroup = DraftGroupNode().apply {
            this.version = draft
            this.title = "root"
            children.add(innerGroup.also { it.parent = this; it.position = 0 })
            children.add(midLeaf.also { it.parent = this; it.position = 1 })
        }
        draft.roots.add(rootGroup)

        val domain = mapper.toDomain(draft)

        assertEquals(1, domain.roots.size)
        val root = domain.roots[0] as EstimationGroup
        assertEquals("root", root.title)
        assertEquals(2, root.children.size)

        val inner = root.children[0] as EstimationGroup
        assertEquals("inner", inner.title)
        assertEquals(1, inner.children.size)

        val deep = inner.children[0] as EstimationItem
        assertEquals("deep", deep.description)
        assertTrue(deep is FixedEstimationItem)

        val mid = root.children[1] as EstimationItem
        assertEquals("mid", mid.description)
    }

    @Test
    fun `time-relative leaf round-trips to TimeRelativeEstimationItem`() {
        val draft = DraftEstimationVersion().apply { versionNumber = 1 }
        val tr = DraftTimeRelativeItemNode().apply {
            this.version = draft
            this.description = "TR"
            this.unit = "h/Woche"
            this.minEffort = 1.0
            this.expectedEffort = 2.0
            this.maxEffort = 3.0
        }
        val group = DraftGroupNode().apply {
            this.version = draft
            this.title = "G"
            children.add(tr.also { it.parent = this; it.position = 0 })
        }
        draft.roots.add(group)

        val domain = mapper.toDomain(draft)
        val domainGroup = domain.roots[0] as EstimationGroup
        val domainTr = domainGroup.children[0] as TimeRelativeEstimationItem
        assertEquals("h/Woche", domainTr.unit)
        assertEquals("TR", domainTr.description)
    }
}
