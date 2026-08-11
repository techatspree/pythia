package io.pythia.service

// Thrown when the uploaded Merlin document's WBS no longer matches the
// estimation tree (task-133). The diff is carried so the REST layer can render
// a 409 the user can act on: cancel, or re-run the export with
// overwriteStructure = true.
class MerlinStructureChangedException(val diff: MerlinExporter.MerlinStructureDiff) :
    RuntimeException(
        "Merlin structure differs from the estimation " +
            "(${diff.missingInMerlin.size} missing in Merlin, " +
            "${diff.missingInEstimation.size} missing in the estimation, " +
            "${diff.reordered.size} reordered)"
    )
