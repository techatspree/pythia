package io.github.theestimator.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "users")
class User : BaseEntity() {

    // TODO: this bounds the user to EntraID. But, the authentication is encapsulated in modules. This need to be adapted.
    @Column(name = "entra_subject_id", unique = true)
    var entraSubjectId: String? = null

    @NotBlank
    @Column(name = "display_name", nullable = false)
    var displayName: String? = null
}
