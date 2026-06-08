package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import jakarta.enterprise.context.ApplicationScoped
import java.io.OutputStream

@ApplicationScoped
class CsvExporter {

    fun export(version: SubmittedEstimationVersion, output: OutputStream) {
        val w = output.bufferedWriter(Charsets.UTF_8)
        fun cell(s: String): String =
            if (s.any { it == ',' || it == '"' || it == '\n' })
                "\"" + s.replace("\"", "\"\"") + "\""
            else s

        // Header: Path,Group,Description,Min,Expected,Max,Mean,OfferPT,Node type
        w.append(
            listOf("Path", "Group", "Description", "Min", "Expected", "Max", "Mean", "OfferPT", "Node type")
                .joinToString(",")
        ).append("\n")

        fun writeNode(node: SubmittedEstimationNode, ancestors: List<String>) {
            val ownLabel = if (node is SubmittedGroupNode) (node.title ?: "") else (node.description ?: "")
            val path = (ancestors + ownLabel).joinToString("/")
            val parentTitle = ancestors.lastOrNull() ?: ""
            val nodeType = when (node) {
                is SubmittedGroupNode -> "GROUP"
                is SubmittedTimeRelativeItemNode -> "TIME_RELATIVE"
                else -> "FIXED"
            }
            val cells = if (node is SubmittedGroupNode) {
                listOf(
                    cell(path),
                    cell(parentTitle),
                    cell(node.title ?: ""),
                    "", "", "",
                    node.mean.toString(),
                    node.offerPT.toString(),
                    nodeType
                )
            } else {
                listOf(
                    cell(path),
                    cell(parentTitle),
                    cell(node.description ?: ""),
                    (node.minEffort ?: 0.0).toString(),
                    (node.expectedEffort ?: 0.0).toString(),
                    (node.maxEffort ?: 0.0).toString(),
                    node.mean.toString(),
                    node.offerPT.toString(),
                    nodeType
                )
            }
            w.append(cells.joinToString(",")).append("\n")

            if (node is SubmittedGroupNode) {
                val childAncestors = ancestors + (node.title ?: "")
                node.children.forEach { writeNode(it, childAncestors) }
            }
        }

        version.roots.forEach { writeNode(it, ancestors = emptyList()) }

        // Totals row: 9 columns. Path empty, Group="Total", four empty cells,
        // empty Mean cell, the total in the OfferPT column, empty Node type.
        // The total stays at column index 7 — see EstimationVersionResourceIT
        // `csv export total matches the version total effort`.
        w.append(",Total,,,,,,${version.totalEffort},\n")
        w.flush()
    }
}
