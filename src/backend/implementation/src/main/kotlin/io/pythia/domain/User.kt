package io.pythia.domain

import io.pythia.i18n.SupportedLanguage
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

    // Persisted UI language preference (ISO 639-1 code, mirrors SupportedLanguage).
    @Column(name = "language", nullable = false)
    var language: String = SupportedLanguage.DE.code
}
