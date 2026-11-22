package io.pythia.rest.dto

import io.pythia.service.MerlinDependencySkipReason
import io.pythia.service.MerlinMergeReport
import org.eclipse.microprofile.openapi.annotations.media.Schema

// Body of a successful merge into Merlin (task-223): the merged document AND
// what the merge changed, in one response — the merge is stateless, so a
// second request would mean a second upload. Paths are WBS paths
// ("Group / Subgroup / Item"), as in MerlinStructureDiffDto.
@Schema(description = "The merged Merlin document and the report of what the merge changed")
data class MerlinMergeResultDto(
    @field:Schema(description = "File name to save the document under, e.g. merlin-v2-merged.zip")
    val fileName: String,
    @field:Schema(description = "The merged Merlin document, standard (padded) base64")
    val document: String,
    val report: MerlinMergeReportDto
)

@Schema(description = "Every change the merge made to the Merlin document")
data class MerlinMergeReportDto(
    @field:Schema(description = "Leaves whose effort changed, in tree order")
    val effortChanges: List<MerlinEffortChangeDto>,
    @field:Schema(description = "Activities created by a structure overwrite")
    val activitiesAdded: List<String>,
    @field:Schema(description = "Activities deleted by a structure overwrite")
    val activitiesRemoved: List<String>,
    @field:Schema(description = "Activities moved by a structure overwrite")
    val activitiesMoved: List<String>,
    @field:Schema(description = "Pythia dependencies added to the document (finish-to-start)")
    val dependenciesAdded: List<MerlinDependencyDto>,
    @field:Schema(description = "Dependencies deleted together with deleted activities")
    val dependenciesRemoved: List<MerlinDependencyDto>,
    @field:Schema(description = "Pythia dependencies that were not added, with the reason")
    val dependenciesSkipped: List<MerlinSkippedDependencyDto>,
    val contentRemoved: MerlinRemovedContentDto
)

@Schema(description = "One leaf's effort before and after the merge, in person-days; null = no work")
data class MerlinEffortChangeDto(val path: String, val beforePT: Double?, val afterPT: Double?)

@Schema(description = "A dependency between two activities, by WBS path")
data class MerlinDependencyDto(val fromPath: String, val toPath: String)

@Schema(description = "A Pythia dependency the merge did not add")
data class MerlinSkippedDependencyDto(
    val fromPath: String,
    val toPath: String,
    val reason: MerlinDependencySkipReason
)

@Schema(description = "User content deleted together with deleted activities, as counts")
data class MerlinRemovedContentDto(
    val assignments: Int,
    val comments: Int,
    val attachments: Int,
    val costs: Int,
    val customAttributeValues: Int
)

fun MerlinMergeReport.toDto() = MerlinMergeReportDto(
    effortChanges = effortChanges.map { MerlinEffortChangeDto(it.path, it.beforePT, it.afterPT) },
    activitiesAdded = activitiesAdded,
    activitiesRemoved = activitiesRemoved,
    activitiesMoved = activitiesMoved,
    dependenciesAdded = dependenciesAdded.map { MerlinDependencyDto(it.fromPath, it.toPath) },
    dependenciesRemoved = dependenciesRemoved.map { MerlinDependencyDto(it.fromPath, it.toPath) },
    dependenciesSkipped = dependenciesSkipped.map { MerlinSkippedDependencyDto(it.fromPath, it.toPath, it.reason) },
    contentRemoved = MerlinRemovedContentDto(
        assignments = contentRemoved.assignments,
        comments = contentRemoved.comments,
        attachments = contentRemoved.attachments,
        costs = contentRemoved.costs,
        customAttributeValues = contentRemoved.customAttributeValues
    )
)
