package io.github.theestimator.service

import io.github.theestimator.domain.*
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationVersionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class EstimationVersionServiceTest {

    private lateinit var versionRepository: EstimationVersionRepository
    private lateinit var estimationRepository: EstimationRepository
    private lateinit var calculator: EstimationCalculator
    private lateinit var importer: ExcelImporter
    private lateinit var auditLogService: AuditLogService
    private lateinit var service: EstimationVersionService

    private lateinit var estimation: Estimation

    @BeforeEach
    fun setup() {
        versionRepository = mock {
            on { persist(any<EstimationVersion>()) } doAnswer { }
        }
        estimationRepository = mock()
        calculator = EstimationCalculator()
        importer = ExcelImporter()
        auditLogService = mock()

        service = EstimationVersionService(
            versionRepository, estimationRepository, calculator, importer, auditLogService
        )

        estimation = Estimation().apply {
            id = UUID.randomUUID()
            offer = "Test Offer"
            project = Project().apply { name = "Test Project" }
        }
    }

    @Test
    fun `createNewVersion with no existing version creates version 1`() {
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(null)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(1, version.versionNumber)
        assertEquals(EstimationVersionStatus.DRAFT, version.status)
        assertEquals(estimation, version.estimation)
        verify(versionRepository).persist(version)
        verify(auditLogService).log(eq(null), eq("EstimationVersion"), anyOrNull(), eq("CREATE"), any())
    }

    @Test
    fun `createNewVersion increments version number`() {
        val existingVersion = createTestVersion(estimation, 3)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(4, version.versionNumber)
    }

    @Test
    fun `createNewVersion deep-clones parameters`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(existingVersion.parameters.size, version.parameters.size)
        version.parameters.forEach { param ->
            assertNull(param.id)
            assertEquals(version, param.version)
        }
        val stdDev = version.parameters.find { it.name == "Standardabweichungsfaktor" }
        assertNotNull(stdDev)
        assertEquals(2.0, stdDev!!.value)
    }

    @Test
    fun `createNewVersion deep-clones effort drivers`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(existingVersion.effortDrivers.size, version.effortDrivers.size)
        version.effortDrivers.forEach { driver ->
            assertNull(driver.id)
            assertEquals(version, driver.version)
        }
    }

    @Test
    fun `createNewVersion deep-clones phases`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(existingVersion.phases.size, version.phases.size)
        version.phases.forEach { phase ->
            assertNull(phase.id)
            assertEquals(version, phase.version)
        }
    }

    @Test
    fun `createNewVersion deep-clones item groups and items`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(existingVersion.itemGroups.size, version.itemGroups.size)
        val sourceItems = existingVersion.itemGroups.flatMap { it.items }
        val clonedItems = version.itemGroups.flatMap { it.items }
        assertEquals(sourceItems.size, clonedItems.size)
        clonedItems.forEach { item ->
            assertNull(item.id)
            assertNotNull(item.group)
            assertEquals(version, item.group!!.version)
        }
    }

    @Test
    fun `createNewVersion deep-clones additional costs`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        assertEquals(existingVersion.additionalCosts.size, version.additionalCosts.size)
        version.additionalCosts.forEach { cost ->
            assertNull(cost.id)
            assertEquals(version, cost.version)
        }
    }

    @Test
    fun `createNewVersion recalculates derived fields`() {
        val existingVersion = createTestVersion(estimation, 1)
        whenever(estimationRepository.findById(estimation.id!!)).thenReturn(estimation)
        whenever(versionRepository.findLatestByEstimationId(estimation.id!!)).thenReturn(existingVersion)

        val version = service.createNewVersion(estimation.id!!)

        val items = version.itemGroups.flatMap { it.items }
        items.forEach { item ->
            assertNotNull(item.mean)
            assertNotNull(item.offerPT)
        }
        assertNotNull(version.totalEffort)
    }

    @Test
    fun `submit transitions DRAFT to SUBMITTED`() {
        val version = createTestVersion(estimation, 1).apply { id = UUID.randomUUID() }
        whenever(versionRepository.findById(version.id!!)).thenReturn(version)

        val result = service.submit(version.id!!)

        assertEquals(EstimationVersionStatus.SUBMITTED, result.status)
        verify(auditLogService).log(eq(null), eq("EstimationVersion"), eq(version.id), eq("SUBMIT"), any())
    }

    @Test
    fun `submit sets currentVersion on estimation`() {
        val version = createTestVersion(estimation, 1).apply { id = UUID.randomUUID() }
        whenever(versionRepository.findById(version.id!!)).thenReturn(version)

        service.submit(version.id!!)

        assertEquals(version, estimation.currentVersion)
    }

    @Test
    fun `submit rejects non-DRAFT version`() {
        val version = createTestVersion(estimation, 1).apply {
            id = UUID.randomUUID()
            status = EstimationVersionStatus.SUBMITTED
        }
        whenever(versionRepository.findById(version.id!!)).thenReturn(version)

        val exception = assertThrows<IllegalStateException> {
            service.submit(version.id!!)
        }
        assertTrue(exception.message!!.contains("SUBMITTED"))
    }

    @Test
    fun `ensureDraft throws for SUBMITTED version`() {
        val version = createTestVersion(estimation, 1).apply {
            status = EstimationVersionStatus.SUBMITTED
        }

        assertThrows<IllegalStateException> {
            service.ensureDraft(version)
        }
    }

    @Test
    fun `ensureDraft passes for DRAFT version`() {
        val version = createTestVersion(estimation, 1)

        assertDoesNotThrow {
            service.ensureDraft(version)
        }
    }

    @Test
    fun `createNewVersion throws for unknown estimation`() {
        val unknownId = UUID.randomUUID()
        whenever(estimationRepository.findById(unknownId)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            service.createNewVersion(unknownId)
        }
    }

    private fun createTestVersion(estimation: Estimation, versionNumber: Int): EstimationVersion {
        val version = EstimationVersion().apply {
            id = UUID.randomUUID()
            this.estimation = estimation
            this.versionNumber = versionNumber
            this.status = EstimationVersionStatus.DRAFT
        }

        version.parameters.addAll(listOf(
            EstimationParameter().apply {
                id = UUID.randomUUID(); name = "Standardabweichungsfaktor"; value = 2.0; this.version = version
            },
            EstimationParameter().apply {
                id = UUID.randomUUID(); name = "Tagessatz"; value = 800.0; this.version = version
            },
            EstimationParameter().apply {
                id = UUID.randomUUID(); name = "Vertriebszuschlag"; value = 0.1; this.version = version
            }
        ))

        version.effortDrivers.add(EffortDriver().apply {
            id = UUID.randomUUID(); description = "Gewährleistung"; factor = 0.05; this.version = version
        })

        val phase = ProjectPhase().apply {
            id = UUID.randomUUID(); name = "Phase 1"; abbreviation = "P1"; durationWeeks = 9.0; this.version = version
        }
        version.phases.add(phase)

        val group = EstimationItemGroup().apply {
            id = UUID.randomUUID(); title = "U01: Feature"; this.phase = phase; this.version = version
        }
        version.itemGroups.add(group)

        group.items.addAll(listOf(
            FixedEstimationItem().apply {
                id = UUID.randomUUID(); description = "Task A"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0
                this.phase = phase; this.group = group
            },
            FixedEstimationItem().apply {
                id = UUID.randomUUID(); description = "Task B"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0
                this.phase = phase; this.group = group
            }
        ))

        version.additionalCosts.add(AdditionalCost().apply {
            id = UUID.randomUUID(); description = "Server"; amount = 2500.0; type = AdditionalCostType.ONE_TIME
            this.phase = phase; this.version = version
        })

        return version
    }
}
