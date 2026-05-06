package io.github.theestimator.domain.draft

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("FIXED")
class DraftFixedEstimationItem : DraftEstimationItem()
