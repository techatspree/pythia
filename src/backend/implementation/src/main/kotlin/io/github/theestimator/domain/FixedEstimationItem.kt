package io.github.theestimator.domain

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("FIXED")
class FixedEstimationItem : EstimationItem()
