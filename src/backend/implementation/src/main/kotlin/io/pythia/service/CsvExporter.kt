package io.pythia.service

import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedGroupNode
import io.pythia.domain.submitted.SubmittedTimeRelativeItemNode
import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodRegistry
import jakarta.enterprise.context.ApplicationScoped
import java.io.BufferedWriter
import java.io.OutputStream

@ApplicationScoped
class CsvExporter {

    fun export(version: SubmittedEstimationVersion, output: OutputStream) {
        val w = output.bufferedWriter(Charsets.UTF_8)

        // Header: Path,Group,Description,<method columns>,Mean,OfferPT,Node type.
        // The method-specific columns (Min,Expected,Max for PERT) are the single
        // source of the column shape — sourced from the SPI module (task-098).
        val methodColumns = EstimationMethodRegistry
            .require(EstimationMethod.THREE_POINT_PERT)
            .exportColumnHeaders()
        w.append(
            (listOf("Path", "Group", "Description") + methodColumns + listOf("Mean", "OfferPT", "Node type"))
                .joinToString(",")
        ).append("\n")

        version.roots.forEach { writeNode(w, it, ancestors = emptyList()) }

        // Totals row: 9 columns. Path empty, Group="Total", four empty cells,
        // empty Mean cell, the total in the OfferPT column, empty Node type.
        // The total stays at column index 7 — see EstimationVersionResourceIT
        // `csv export total matches the version total effort`.
        w.append(",Total,,,,,,${version.totalEffort},\n")
        w.flush()
    }

    private fun cell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }

    private fun nodeCells(node: SubmittedEstimationNode, path: String, parentTitle: String): List<String> {
        val nodeType = when (node) {
            is SubmittedGroupNode -> "GROUP"
            is SubmittedTimeRelativeItemNode -> "TIME_RELATIVE"
            else -> "FIXED"
        }
        return if (node is SubmittedGroupNode) {
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
    }

    private fun writeNode(w: BufferedWriter, node: SubmittedEstimationNode, ancestors: List<String>) {
        val ownLabel = if (node is SubmittedGroupNode) (node.title ?: "") else (node.description ?: "")
        val path = (ancestors + ownLabel).joinToString("/")
        val parentTitle = ancestors.lastOrNull() ?: ""
        w.append(nodeCells(node, path, parentTitle).joinToString(",")).append("\n")

        if (node is SubmittedGroupNode) {
            val childAncestors = ancestors + (node.title ?: "")
            node.children.forEach { writeNode(w, it, childAncestors) }
        }
    }
}
