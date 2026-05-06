package io.github.theestimator.domain.submitted

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("TIME_RELATIVE")
class SubmittedTimeRelativeEstimationItem : SubmittedEstimationItem() {

    var unit: String? = null
}
