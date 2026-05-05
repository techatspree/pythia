package io.github.theestimator.rest

import io.github.theestimator.domain.EstimationVersionStatus
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationVersionRepository
import io.github.theestimator.rest.dto.*
import io.github.theestimator.service.EstimationCalculator
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ExcelExporter
import io.github.theestimator.service.VersionComparisonService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.jboss.resteasy.reactive.RestForm
import java.io.ByteArrayOutputStream
import java.util.UUID

@Path("/api/estimations/{estimationId}/versions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class EstimationVersionResource(
    private val versionService: EstimationVersionService,
    private val estimationRepository: EstimationRepository,
    private val versionRepository: EstimationVersionRepository,
    private val excelExporter: ExcelExporter,
    private val calculator: EstimationCalculator,
    private val comparisonService: VersionComparisonService
) {

    @GET
    fun listVersions(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val versions = versionService.findByEstimationId(estimationId)
        return Response.ok(versions.map { it.toSummaryDto() }).build()
    }

    @POST
    @Transactional
    fun createVersion(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val version = versionService.createNewVersion(estimationId)
        return Response.status(Response.Status.CREATED).entity(version.toDto()).build()
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    fun importFromExcel(
        @PathParam("estimationId") estimationId: UUID,
        @RestForm("file") file: FileUpload
    ): Response {
        ensureEstimationExists(estimationId)
        val version = versionService.importFromExcel(estimationId, file.uploadedFile().toFile().inputStream())
        return Response.status(Response.Status.CREATED).entity(version.toDto()).build()
    }

    @GET
    @Path("/{versionNumber}")
    fun getVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int
    ): Response {
        val version = findVersion(estimationId, versionNumber)
        return Response.ok(version.toDto()).build()
    }

    @PUT
    @Path("/{versionNumber}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    fun updateVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int,
        update: EstimationVersionUpdateDto
    ): Response {
        val version = findVersion(estimationId, versionNumber)
        if (version.status != EstimationVersionStatus.DRAFT) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to "Cannot edit version in status ${version.status}"))
                .build()
        }

        update.notes?.let { version.notes = it }

        update.parameters?.let { params ->
            version.parameters.clear()
            params.forEach { dto ->
                version.parameters.add(io.github.theestimator.domain.EstimationParameter().apply {
                    name = dto.name
                    value = dto.value
                    comment = dto.comment
                    this.version = version
                })
            }
        }

        update.effortDrivers?.let { drivers ->
            version.effortDrivers.clear()
            drivers.forEach { dto ->
                version.effortDrivers.add(io.github.theestimator.domain.EffortDriver().apply {
                    description = dto.description
                    factor = dto.factor
                    comment = dto.comment
                    this.version = version
                })
            }
        }

        calculator.calculate(version)
        return Response.ok(version.toDto()).build()
    }

    @POST
    @Path("/{versionNumber}/submit")
    @Transactional
    fun submitVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int
    ): Response {
        val version = findVersion(estimationId, versionNumber)
        if (version.status != EstimationVersionStatus.DRAFT) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to "Cannot submit version in status ${version.status}"))
                .build()
        }
        val submitted = versionService.submit(version.id!!)
        return Response.ok(submitted.toDto()).build()
    }

    @GET
    @Path("/{versionNumber}/export")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    fun exportVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int
    ): Response {
        val version = findVersion(estimationId, versionNumber)
        val output = ByteArrayOutputStream()
        excelExporter.export(version, output)
        return Response.ok(output.toByteArray())
            .header("Content-Disposition", "attachment; filename=\"estimation_v${versionNumber}.xlsx\"")
            .build()
    }

    @DELETE
    @Path("/{versionNumber}")
    @Transactional
    fun deleteVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int
    ): Response {
        val version = findVersion(estimationId, versionNumber)
        if (version.status != EstimationVersionStatus.DRAFT) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to "Cannot delete version in status ${version.status}"))
                .build()
        }
        versionRepository.delete(version)
        return Response.noContent().build()
    }

    @GET
    @Path("/{versionA}/compare/{versionB}")
    fun compareVersions(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionA") versionA: Int,
        @PathParam("versionB") versionB: Int
    ): Response {
        val verA = findVersion(estimationId, versionA)
        val verB = findVersion(estimationId, versionB)
        val comparison = comparisonService.compare(verA, verB)
        return Response.ok(comparison).build()
    }

    private fun ensureEstimationExists(estimationId: UUID) {
        estimationRepository.findById(estimationId)
            ?: throw NotFoundException("Estimation not found: $estimationId")
    }

    private fun findVersion(estimationId: UUID, versionNumber: Int): io.github.theestimator.domain.EstimationVersion {
        ensureEstimationExists(estimationId)
        val versions = versionService.findByEstimationId(estimationId)
        return versions.find { it.versionNumber == versionNumber }
            ?: throw NotFoundException("Version $versionNumber not found for estimation $estimationId")
    }
}
