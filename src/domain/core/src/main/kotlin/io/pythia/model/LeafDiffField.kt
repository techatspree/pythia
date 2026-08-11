package io.pythia.model

/**
 * One comparable field of a leaf, named by the method that owns the leaf.
 *
 * This is how `DiffSummary` stays method-agnostic: instead of branching on
 * concrete leaf types (which would make core depend on the method modules), each
 * leaf declares the ordered list of fields it wants diffed via
 * [EstimationItem.diffFields].
 *
 * Numbers stay `Double` rather than pre-formatted strings so the comparison
 * still happens on the value — `DiffSummary` owns the locale-neutral
 * stringification and applies it only once a change is detected.
 *
 * Domain-internal: not `@JsExport`ed, nothing on the wire.
 */
class LeafDiffField private constructor(
    val name: String,
    val numeric: Boolean,
    val text: String?,
    val number: Double?
) {
    companion object {
        fun text(name: String, value: String?): LeafDiffField =
            LeafDiffField(name, numeric = false, text = value, number = null)

        fun number(name: String, value: Double?): LeafDiffField =
            LeafDiffField(name, numeric = true, text = null, number = value)
    }
}
