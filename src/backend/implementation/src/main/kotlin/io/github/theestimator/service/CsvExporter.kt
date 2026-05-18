package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
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
        w.append(
            listOf("Group", "Description", "Min", "Expected", "Max", "Mean", "OfferPT")
                .joinToString(",")
        ).append("\n")
        version.itemGroups.forEach { g ->
            g.items.forEach { i ->
                w.append(
                    listOf(
                        cell(g.title), cell(i.description),
                        i.minEffort.toString(), i.expectedEffort.toString(),
                        i.maxEffort.toString(), i.mean.toString(),
                        i.offerPT.toString()
                    ).joinToString(",")
                ).append("\n")
            }
        }
        w.append("Total,,,,,,${version.totalEffort}\n")
        w.flush()
    }
}
