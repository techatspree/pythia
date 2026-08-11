@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import io.pythia.method.EstimationMethod
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// The constructor parameters are the leaf entity's fields; this is an
// @JsExport @DomainEntity whose shape is part of the public domain API.
@Suppress("LongParameterList")
@JsExport
@DomainEntity
abstract class EstimationItem(
    val description: String,
    val code: String = "",
    val minEffort: Double = 0.0,
    val expectedEffort: Double = 0.0,
    val maxEffort: Double = 0.0,
    val assumptions: String = "",
    val phase: ProjectPhase? = null,
    logicalId: String = newId(),
    val calculationParameters: CalculationParameters = CalculationParameters(),
    id: String? = null,
    createdAt: String? = null,
    updatedAt: String? = null
) : EstimationNode(logicalId, id, createdAt, updatedAt) {

    override val mean: Double
        get() = PertCalculation.mean(minEffort, expectedEffort, maxEffort)

    override val variance: Double
        get() = PertCalculation.variance(minEffort, maxEffort)

    override val riskSurcharge: Double
        get() = mean * calculationParameters.riskFactor

    override val driverSurcharge: Double
        get() = mean * calculationParameters.totalDriverFactor

    override val offerPT: Double
        get() = mean + riskSurcharge + driverSurcharge

    override val cost: Double
        get() = offerPT * calculationParameters.dailyRate

    override val offerPrice: Double
        get() = cost * (1 + calculationParameters.salesSurcharge)

    abstract override fun withCalculationParameters(params: CalculationParameters): EstimationItem

    /**
     * The estimation method this leaf belongs to. Declared by the leaf so core
     * never has to sniff concrete types to work out a version's method — see
     * `EstimationVersion.calculate()`.
     */
    abstract val method: EstimationMethod

    /** Wire/diff discriminator for this leaf (`FIXED` / `TIME_RELATIVE` / `BUCKETED`). */
    abstract val nodeTypeLabel: String

    /**
     * The ordered fields `DiffSummary` should compare for this leaf. Declared
     * here so core stays method-agnostic; a leaf with method-specific inputs
     * overrides this with its own list.
     *
     * Order is part of the output — the history panel renders these in sequence.
     *
     * `@JsExport.Ignore`d: this is a domain-internal diff hook, and exporting it
     * would drag the non-exportable [LeafDiffField] into `domain.d.mts`. It
     * cannot be `internal` instead — the leaves that override it live in other
     * Gradle modules.
     */
    @JsExport.Ignore
    open fun diffFields(): List<LeafDiffField> = listOf(
        LeafDiffField.number("optimistic", minEffort),
        LeafDiffField.number("likely", expectedEffort),
        LeafDiffField.number("pessimistic", maxEffort),
        LeafDiffField.text("assumptions", assumptions.ifBlank { null }),
        LeafDiffField.text("phase", phase?.abbreviation)
    )
}
