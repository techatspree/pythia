package io.github.theestimator.domain.draft

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("TIME_RELATIVE")
class DraftTimeRelativeEstimationItem : DraftEstimationItem() {

    var unit: String? = null
}
