package io.github.theestimator.rest.dto

import org.eclipse.microprofile.openapi.annotations.media.Schema

// Body of a 409 from a Merlin export whose target document has drifted from the
// estimation tree (task-133). Paths are WBS paths ("Group / Subgroup / Item").
@Schema(description = "How the Merlin document's WBS differs from the estimation tree")
data class MerlinStructureDiffDto(
    @field:Schema(description = "Paths the estimation has that the Merlin document lacks")
    val missingInMerlin: List<String>,
    @field:Schema(description = "Paths the Merlin document has that the estimation lacks")
    val missingInEstimation: List<String>,
    @field:Schema(description = "Paths present on both sides but at a different sibling position")
    val reordered: List<String>,
    @field:Schema(description = "True when the structures match and the export can proceed")
    val inSync: Boolean
)
