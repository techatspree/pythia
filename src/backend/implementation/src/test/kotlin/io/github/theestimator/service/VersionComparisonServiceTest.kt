package io.github.theestimator.service

import io.github.theestimator.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VersionComparisonServiceTest {

    private lateinit var service: VersionComparisonService

    @BeforeEach
    fun setup() {
        service = VersionComparisonService()
    }

    @Test
    fun `identical versions produce empty comparison`() {
        val logicalId = UUID.randomUUID()
        val groupLogicalId = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId, listOf(
            createItem(logicalId, "Task A", 1.0, 2.0, 3.0)
        ))
        val versionB = createVersion(2, groupLogicalId, listOf(
            createItem(logicalId, "Task A", 1.0, 2.0, 3.0)
        ))

        val result = service.compare(versionA, versionB)

        assertTrue(result.addedItems.isEmpty())
        assertTrue(result.removedItems.isEmpty())
        assertTrue(result.modifiedItems.isEmpty())
        assertTrue(result.addedGroups.isEmpty())
        assertTrue(result.removedGroups.isEmpty())
        assertTrue(result.parameterChanges.isEmpty())
    }

    @Test
    fun `added item is detected`() {
        val logicalId1 = UUID.randomUUID()
        val logicalId2 = UUID.randomUUID()
        val groupLogicalId = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId, listOf(
            createItem(logicalId1, "Task A", 1.0, 2.0, 3.0)
        ))
        val versionB = createVersion(2, groupLogicalId, listOf(
            createItem(logicalId1, "Task A", 1.0, 2.0, 3.0),
            createItem(logicalId2, "Task B", 2.0, 3.0, 5.0)
        ))

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.addedItems.size)
        assertEquals(logicalId2, result.addedItems[0].logicalId)
        assertEquals("Task B", result.addedItems[0].description)
    }

    @Test
    fun `removed item is detected`() {
        val logicalId1 = UUID.randomUUID()
        val logicalId2 = UUID.randomUUID()
        val groupLogicalId = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId, listOf(
            createItem(logicalId1, "Task A", 1.0, 2.0, 3.0),
            createItem(logicalId2, "Task B", 2.0, 3.0, 5.0)
        ))
        val versionB = createVersion(2, groupLogicalId, listOf(
            createItem(logicalId1, "Task A", 1.0, 2.0, 3.0)
        ))

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.removedItems.size)
        assertEquals(logicalId2, result.removedItems[0].logicalId)
    }

    @Test
    fun `modified item is detected with changed fields`() {
        val logicalId = UUID.randomUUID()
        val groupLogicalId = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId, listOf(
            createItem(logicalId, "Task A", 1.0, 2.0, 3.0)
        ))
        val versionB = createVersion(2, groupLogicalId, listOf(
            createItem(logicalId, "Task A", 2.0, 4.0, 6.0)
        ))

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.modifiedItems.size)
        val mod = result.modifiedItems[0]
        assertEquals(logicalId, mod.logicalId)
        assertTrue(mod.changedFields.contains("minEffort"))
        assertTrue(mod.changedFields.contains("expectedEffort"))
        assertTrue(mod.changedFields.contains("maxEffort"))
    }

    @Test
    fun `added group is detected`() {
        val groupLogicalId1 = UUID.randomUUID()
        val groupLogicalId2 = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId1, emptyList())
        val versionB = createVersionWithGroups(2, listOf(
            createGroup(groupLogicalId1, "Group 1", emptyList()),
            createGroup(groupLogicalId2, "Group 2", emptyList())
        ))

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.addedGroups.size)
        assertEquals(groupLogicalId2, result.addedGroups[0].logicalId)
    }

    @Test
    fun `removed group is detected`() {
        val groupLogicalId1 = UUID.randomUUID()
        val groupLogicalId2 = UUID.randomUUID()

        val versionA = createVersionWithGroups(1, listOf(
            createGroup(groupLogicalId1, "Group 1", emptyList()),
            createGroup(groupLogicalId2, "Group 2", emptyList())
        ))
        val versionB = createVersion(2, groupLogicalId1, emptyList())

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.removedGroups.size)
        assertEquals(groupLogicalId2, result.removedGroups[0].logicalId)
    }

    @Test
    fun `added parameter is detected`() {
        val groupLogicalId = UUID.randomUUID()
        val versionA = createVersion(1, groupLogicalId, emptyList())
        val versionB = createVersion(2, groupLogicalId, emptyList()).also {
            it.parameters.add(EstimationParameter().apply {
                name = "NewParam"; value = 5.0; version = it
            })
        }

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.parameterChanges.size)
        assertEquals("NewParam", result.parameterChanges[0].name)
        assertEquals("ADDED", result.parameterChanges[0].changeType)
        assertEquals(5.0, result.parameterChanges[0].newValue)
    }

    @Test
    fun `removed parameter is detected`() {
        val groupLogicalId = UUID.randomUUID()
        val versionA = createVersion(1, groupLogicalId, emptyList()).also {
            it.parameters.add(EstimationParameter().apply {
                name = "OldParam"; value = 3.0; version = it
            })
        }
        val versionB = createVersion(2, groupLogicalId, emptyList())

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.parameterChanges.size)
        assertEquals("OldParam", result.parameterChanges[0].name)
        assertEquals("REMOVED", result.parameterChanges[0].changeType)
    }

    @Test
    fun `modified parameter is detected`() {
        val groupLogicalId = UUID.randomUUID()
        val versionA = createVersion(1, groupLogicalId, emptyList()).also {
            it.parameters.add(EstimationParameter().apply {
                name = "Rate"; value = 800.0; version = it
            })
        }
        val versionB = createVersion(2, groupLogicalId, emptyList()).also {
            it.parameters.add(EstimationParameter().apply {
                name = "Rate"; value = 1000.0; version = it
            })
        }

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.parameterChanges.size)
        val change = result.parameterChanges[0]
        assertEquals("Rate", change.name)
        assertEquals("MODIFIED", change.changeType)
        assertEquals(800.0, change.oldValue)
        assertEquals(1000.0, change.newValue)
    }

    @Test
    fun `description change is detected`() {
        val logicalId = UUID.randomUUID()
        val groupLogicalId = UUID.randomUUID()

        val versionA = createVersion(1, groupLogicalId, listOf(
            createItem(logicalId, "Old name", 1.0, 2.0, 3.0)
        ))
        val versionB = createVersion(2, groupLogicalId, listOf(
            createItem(logicalId, "New name", 1.0, 2.0, 3.0)
        ))

        val result = service.compare(versionA, versionB)

        assertEquals(1, result.modifiedItems.size)
        assertTrue(result.modifiedItems[0].changedFields.contains("description"))
    }

    private fun createVersion(
        versionNumber: Int,
        groupLogicalId: UUID,
        items: List<EstimationItem>
    ): EstimationVersion {
        val version = EstimationVersion().apply {
            this.versionNumber = versionNumber
            this.status = EstimationVersionStatus.DRAFT
        }
        val group = createGroup(groupLogicalId, "Test Group", items)
        group.version = version
        items.forEach { it.group = group }
        version.itemGroups.add(group)
        return version
    }

    private fun createVersionWithGroups(
        versionNumber: Int,
        groups: List<EstimationItemGroup>
    ): EstimationVersion {
        val version = EstimationVersion().apply {
            this.versionNumber = versionNumber
            this.status = EstimationVersionStatus.DRAFT
        }
        groups.forEach { group ->
            group.version = version
            group.items.forEach { it.group = group }
            version.itemGroups.add(group)
        }
        return version
    }

    private fun createGroup(
        logicalId: UUID,
        title: String,
        items: List<EstimationItem>
    ): EstimationItemGroup {
        return EstimationItemGroup().apply {
            this.logicalId = logicalId
            this.title = title
            this.items.addAll(items)
        }
    }

    private fun createItem(
        logicalId: UUID,
        description: String,
        min: Double,
        expected: Double,
        max: Double
    ): FixedEstimationItem {
        return FixedEstimationItem().apply {
            this.logicalId = logicalId
            this.description = description
            this.minEffort = min
            this.expectedEffort = expected
            this.maxEffort = max
        }
    }
}
