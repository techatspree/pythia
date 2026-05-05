package io.github.theestimator.service

import io.github.theestimator.domain.*
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

@QuarkusTest
@Transactional
class EstimationVersionServiceIT {

    @Inject
    lateinit var versionService: EstimationVersionService

    @Inject
    lateinit var estimationService: EstimationService

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    private lateinit var project: Project
    private lateinit var estimation: Estimation

    @BeforeEach
    fun setup() {
        project = Project().apply { name = "IT Test Project" }
        projectRepository.persist(project)
        estimation = estimationService.create("IT Test Offer", project)
    }

    @Test
    fun `createNewVersion on empty estimation creates version 1`() {
        val version = versionService.createNewVersion(estimation.id!!)

        assertNotNull(version.id)
        assertEquals(1, version.versionNumber)
        assertEquals(EstimationVersionStatus.DRAFT, version.status)
        assertEquals(estimation.id, version.estimation?.id)
    }

    @Test
    fun `createNewVersion increments version number`() {
        versionService.createNewVersion(estimation.id!!)
        val v2 = versionService.createNewVersion(estimation.id!!)

        assertEquals(2, v2.versionNumber)
    }

    @Test
    fun `createNewVersion deep-clones from latest version`() {
        val file = resolveReferenceSpreadsheet() ?: return
        val v1 = versionService.importFromExcel(estimation.id!!, file.inputStream())

        val v2 = versionService.createNewVersion(estimation.id!!)

        assertEquals(v1.parameters.size, v2.parameters.size)
        assertEquals(v1.effortDrivers.size, v2.effortDrivers.size)
        assertEquals(v1.phases.size, v2.phases.size)
        assertEquals(v1.itemGroups.size, v2.itemGroups.size)
        assertEquals(v1.additionalCosts.size, v2.additionalCosts.size)

        val v1Items = v1.itemGroups.flatMap { it.items }
        val v2Items = v2.itemGroups.flatMap { it.items }
        assertEquals(v1Items.size, v2Items.size)

        assertNotEquals(v1.id, v2.id)
        v2.parameters.forEach { assertNotNull(it.id) }
    }

    @Test
    fun `importFromExcel creates version with calculated fields`() {
        val file = resolveReferenceSpreadsheet() ?: return
        val version = versionService.importFromExcel(estimation.id!!, file.inputStream())

        assertNotNull(version.id)
        assertEquals(1, version.versionNumber)
        assertNotNull(version.totalEffort)
        assertTrue(version.totalEffort!! > 0)

        val items = version.itemGroups.flatMap { it.items }
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertNotNull(item.mean)
            assertNotNull(item.offerPT)
        }
    }

    @Test
    fun `submit transitions DRAFT to SUBMITTED`() {
        val version = versionService.createNewVersion(estimation.id!!)

        val submitted = versionService.submit(version.id!!)

        assertEquals(EstimationVersionStatus.SUBMITTED, submitted.status)
    }

    @Test
    fun `submit sets currentVersion on estimation`() {
        val version = versionService.createNewVersion(estimation.id!!)
        versionService.submit(version.id!!)

        val refreshed = estimationRepository.findById(estimation.id!!)
        assertNotNull(refreshed?.currentVersion)
        assertEquals(version.id, refreshed?.currentVersion?.id)
    }

    @Test
    fun `submit rejects non-DRAFT version`() {
        val version = versionService.createNewVersion(estimation.id!!)
        versionService.submit(version.id!!)

        assertThrows<IllegalStateException> {
            versionService.submit(version.id!!)
        }
    }

    @Test
    fun `ensureDraft throws for SUBMITTED version`() {
        val version = versionService.createNewVersion(estimation.id!!)
        versionService.submit(version.id!!)

        val found = versionService.findById(version.id!!)!!
        assertThrows<IllegalStateException> {
            versionService.ensureDraft(found)
        }
    }

    @Test
    fun `findByEstimationId returns all versions`() {
        versionService.createNewVersion(estimation.id!!)
        versionService.createNewVersion(estimation.id!!)

        val versions = versionService.findByEstimationId(estimation.id!!)
        assertEquals(2, versions.size)
    }

    private fun resolveReferenceSpreadsheet(): File? {
        val file = File("../../planning/inputdata/reference-spreadsheet.xlsx")
        if (file.exists()) return file
        val absFile = File(System.getProperty("user.dir") + "/../../../planning/inputdata/reference-spreadsheet.xlsx")
        if (absFile.exists()) return absFile
        return null
    }
}
