package io.pythia.service

import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedGroupNode
import io.pythia.domain.submitted.SubmittedTimeRelativeItemNode
import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodModule
import io.pythia.method.EstimationMethodRegistry
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.io.BufferedWriter
import java.io.OutputStream

@ApplicationScoped
class CsvExporter {

    fun export(version: SubmittedEstimationVersion, method: EstimationMethod, output: OutputStream) {
        val w = output.bufferedWriter(Charsets.UTF_8)

        // Header: Path,Group,Description,<method columns>,Mean,OfferPT,Node type.
        // The method columns come from the estimation's OWN method (task-107;
        // task-098 introduced the lookup but pinned it to PERT). Their POSITION is
        // frozen — this layout is what consumers read — but their NUMBER is not:
        // PERT contributes 3 (Min/Expected/Max) and bucket+sampled 5
        // (Bucket/Is Sample/Optimistic/Likely/Pessimistic), so nothing below may
        // assume a count.
        val module = EstimationMethodRegistry.require(method)
        val methodColumns = module.exportColumnHeaders()
        val leading = listOf("Path", "Group", "Description")
        val trailing = listOf("Mean", "OfferPT", "Node type")
        w.append((leading + methodColumns + trailing).joinToString(",")).append("\n")

        var rows = 0
        version.roots.forEach { rows += writeNode(w, it, ancestors = emptyList(), module = module) }

        // Totals row: the total sits in the OfferPT column, whose index moves with
        // the method's column count. This used to be a hand-written run of commas
        // — nine columns with the total pinned at index 7, which was correct only
        // while a method contributed exactly three columns.
        val offerPtIndex = leading.size + methodColumns.size + 1
        val width = leading.size + methodColumns.size + trailing.size
        val totals = MutableList(width) { "" }
        totals[1] = "Total"
        totals[offerPtIndex] = version.totalEffort.toString()
        w.append(totals.joinToString(",")).append("\n")
        w.flush()
        Log.debug("CSV export wrote $rows row(s) for method $method")
    }

    private fun cell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }

    private fun nodeCells(
        node: SubmittedEstimationNode,
        path: String,
        parentTitle: String,
        module: EstimationMethodModule
    ): List<String> {
        val nodeType = when (node) {
            is SubmittedGroupNode -> "GROUP"
            is SubmittedTimeRelativeItemNode -> "TIME_RELATIVE"
            else -> "FIXED"
        }
        // A group carries no method-specific input of its own, so it emits one
        // empty cell per method column — as many as the method actually has.
        val methodCells = if (node is SubmittedGroupNode) {
            List(module.exportColumnHeaders().size) { "" }
        } else {
            module.exportRow(SubmittedItemMapper.toDomain(node)).map { cell(it) }
        }
        val label = if (node is SubmittedGroupNode) (node.title ?: "") else (node.description ?: "")
        return listOf(cell(path), cell(parentTitle), cell(label)) +
            methodCells +
            listOf(node.mean.toString(), node.offerPT.toString(), nodeType)
    }

    private fun writeNode(
        w: BufferedWriter,
        node: SubmittedEstimationNode,
        ancestors: List<String>,
        module: EstimationMethodModule
    ): Int {
        val ownLabel = if (node is SubmittedGroupNode) (node.title ?: "") else (node.description ?: "")
        val path = (ancestors + ownLabel).joinToString("/")
        val parentTitle = ancestors.lastOrNull() ?: ""
        w.append(nodeCells(node, path, parentTitle, module).joinToString(",")).append("\n")

        var rows = 1
        if (node is SubmittedGroupNode) {
            val childAncestors = ancestors + (node.title ?: "")
            node.children.forEach { rows += writeNode(w, it, childAncestors, module) }
        }
        return rows
    }
}
