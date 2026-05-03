package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "users")
class User : BaseEntity() {

    @Column(name = "entra_subject_id", unique = true)
    var entraSubjectId: String? = null

    @NotBlank
    @Column(name = "display_name", nullable = false)
    var displayName: String? = null
}
