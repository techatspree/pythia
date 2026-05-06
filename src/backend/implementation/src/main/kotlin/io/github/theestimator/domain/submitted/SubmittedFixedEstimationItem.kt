package io.github.theestimator.domain.submitted

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("FIXED")
class SubmittedFixedEstimationItem : SubmittedEstimationItem()
