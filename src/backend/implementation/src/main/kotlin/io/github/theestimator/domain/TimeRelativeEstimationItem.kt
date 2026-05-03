package io.github.theestimator.domain

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("TIME_RELATIVE")
class TimeRelativeEstimationItem : EstimationItem() {

    @Column(name = "unit")
    var unit: String? = "h/Woche"
}
