package io.pythia.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import io.pythia.domain.ProjectStatus
import io.pythia.repository.EstimationRepository
import io.pythia.repository.ProjectRepository
import io.pythia.rest.dto.toDetailDto
import io.pythia.rest.dto.toDto
import io.pythia.rest.dto.toEstimationDetailDto
import io.pythia.rest.dto.toSummaryDto
import io.pythia.service.EstimationVersionService
import io.pythia.service.VersionComparisonService
import io.quarkiverse.mcp.server.Tool
import io.quarkiverse.mcp.server.ToolArg
import io.quarkiverse.mcp.server.ToolCallException
import io.quarkus.logging.Log
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.UUID

// The read-only MCP tools (task-218). Each returns the SAME DTO its REST twin
// returns, serialised as JSON with the application's ObjectMapper, so a client
// sees exactly what the UI sees and no number is computed here: the
// calculation stays in the domain, reached through the existing services.
//
// - VIEWER, like every REST read. @Transactional because the detail mappers
//   walk lazy collections.
// - The caller comes from SecurityIdentity, never from the JAX-RS current-user
//   provider: the JAX-RS auth filters fill that one, and an MCP request is a
//   Vert.x route, not JAX-RS, so it is empty here (mcp/CLAUDE.md).
// - canChangeOwner is always false: the MCP surface is read-only.
// - Unknown or malformed input becomes a ToolCallException (an MCP error
//   result) with the message the REST endpoint's 404/400 carries.
@ApplicationScoped
class PythiaMcpTools(
    private val projectRepository: ProjectRepository,
    private val estimationRepository: EstimationRepository,
    private val versionService: EstimationVersionService,
    private val comparisonService: VersionComparisonService,
    private val identity: SecurityIdentity,
    private val objectMapper: ObjectMapper
) {

    @Tool(
        name = "list_projects",
        description = "List the projects in Pythia, optionally only the ACTIVE or ARCHIVED ones."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun listProjects(
        @ToolArg(description = "ACTIVE or ARCHIVED; omit to list all projects", required = false)
        status: String?
    ): String {
        val filter = status?.takeIf { it.isNotBlank() }?.let { parseStatus(it) }
        val projects = if (filter != null) projectRepository.findByStatus(filter) else projectRepository.listAll()
        return respond("list_projects", "status=${filter ?: "any"}", projects.map { it.toSummaryDto(false) })
    }

    @Tool(
        name = "get_project",
        description = "Get one project with its estimations (offers), by project id."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun getProject(@ToolArg(description = "The project's id (a UUID)") projectId: String): String {
        val id = parseId(projectId, "projectId")
        val project = projectRepository.findById(id) ?: toolError("Project not found: $id")
        return respond("get_project", "project=$id", project.toDetailDto(false))
    }

    @Tool(
        name = "get_estimation",
        description = "Get one estimation (an offer within a project) with its live draft total effort."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun getEstimation(@ToolArg(description = "The estimation's id (a UUID)") estimationId: String): String {
        val id = parseId(estimationId, "estimationId")
        val estimation = estimationRepository.findById(id) ?: toolError("Estimation not found: $id")
        val draftTotalEffort = estimation.draftVersion?.let { versionService.calculateDraft(it).totalEffort }
        return respond("get_estimation", "estimation=$id", estimation.toEstimationDetailDto(draftTotalEffort))
    }

    @Tool(
        name = "list_versions",
        description = "List an estimation's versions: the draft first (with its calculated total), then the " +
            "submitted versions."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun listVersions(@ToolArg(description = "The estimation's id (a UUID)") estimationId: String): String {
        val id = existingEstimation(estimationId)
        return respond("list_versions", "estimation=$id", versionService.versionSummaries(id))
    }

    @Tool(
        name = "get_version",
        description = "Get one version of an estimation with all calculated values: the tree of work items, " +
            "efforts, risk, cost and schedule inputs."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun getVersion(
        @ToolArg(description = "The estimation's id (a UUID)") estimationId: String,
        @ToolArg(description = "\"draft\" for the working draft, or a submitted version number such as \"2\"")
        version: String
    ): String {
        val id = existingEstimation(estimationId)
        val dto = if (version == DRAFT) {
            val draft = versionService.findDraft(id) ?: toolError("No draft found for estimation $id")
            draft.toDto(versionService.calculateDraft(draft))
        } else {
            val number = version.toIntOrNull() ?: toolError("Invalid version '$version'")
            val submitted = versionService.findSubmittedVersion(id, number)
                ?: toolError("Version $number not found for estimation $id")
            submitted.toDto()
        }
        return respond("get_version", "estimation=$id version=$version", dto)
    }

    @Tool(
        name = "compare_versions",
        description = "Compare two versions of an estimation: what changed in the items, parameters and totals."
    )
    @RolesAllowed("VIEWER")
    @Transactional
    fun compareVersions(
        @ToolArg(description = "The estimation's id (a UUID)") estimationId: String,
        @ToolArg(description = "The first version: \"draft\" or a version number") versionA: String,
        @ToolArg(description = "The second version: \"draft\" or a version number") versionB: String
    ): String {
        val id = existingEstimation(estimationId)
        val a = resolve(id, versionA)
        val b = resolve(id, versionB)
        return respond("compare_versions", "estimation=$id $versionA vs $versionB", comparisonService.compare(a, b))
    }

    private fun resolve(estimationId: UUID, ref: String) =
        versionService.resolveVersion(estimationId, ref)
            ?: toolError(if (ref == DRAFT) "No draft found" else "Version $ref not found")

    private fun existingEstimation(raw: String): UUID {
        val id = parseId(raw, "estimationId")
        estimationRepository.findById(id) ?: toolError("Estimation not found: $id")
        return id
    }

    private fun respond(tool: String, key: String, result: Any): String {
        Log.info("MCP tool $tool called by ${identity.principal.name} ($key)")
        val json = objectMapper.writeValueAsString(result)
        Log.debug("MCP tool $tool returned ${json.length} characters")
        return json
    }

    private companion object {
        const val DRAFT = "draft"
    }
}

// An MCP error result (not a protocol error) with the REST endpoint's message.
private fun toolError(message: String): Nothing = throw ToolCallException(message)

private fun parseId(raw: String, name: String): UUID =
    runCatching { UUID.fromString(raw.trim()) }.getOrNull()
        ?: toolError("Invalid $name '$raw': expected a UUID")

private fun parseStatus(raw: String): ProjectStatus =
    ProjectStatus.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        ?: toolError("Invalid status '$raw': expected ACTIVE or ARCHIVED")
