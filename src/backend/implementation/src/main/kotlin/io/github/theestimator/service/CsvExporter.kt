package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import jakarta.enterprise.context.ApplicationScoped
import java.io.OutputStream

// task-051 compile shim — task-053 adds depth-aware Path column.
@ApplicationScoped
class CsvExporter {

    fun export(version: SubmittedEstimationVersion, output: OutputStream) {
        val w = output.bufferedWriter(Charsets.UTF_8)
        fun cell(s: String): String =
            if (s.any { it == ',' || it == '"' || it == '\n' })
                "\"" + s.replace("\"", "\"\"") + "\""
            else s
        w.append(
            listOf("Group", "Description", "Min", "Expected", "Max", "Mean", "OfferPT")
                .joinToString(",")
        ).append("\n")
        version.roots.filterIsInstance<SubmittedGroupNode>().forEach { g ->
            collectLeaves(g).forEach { i ->
                w.append(
                    listOf(
                        cell(g.title ?: ""), cell(i.description ?: ""),
                        (i.minEffort ?: 0.0).toString(), (i.expectedEffort ?: 0.0).toString(),
                        (i.maxEffort ?: 0.0).toString(), i.mean.toString(),
                        i.offerPT.toString()
                    ).joinToString(",")
                ).append("\n")
            }
        }
        w.append("Total,,,,,,${version.totalEffort}\n")
        w.flush()
    }

    private fun collectLeaves(node: SubmittedEstimationNode): List<SubmittedEstimationNode> = when (node) {
        is SubmittedGroupNode -> node.children.flatMap { collectLeaves(it) }
        else -> listOf(node)
    }
}
