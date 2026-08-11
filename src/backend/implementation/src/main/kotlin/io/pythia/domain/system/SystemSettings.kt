package io.pythia.domain.system

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

// Per-installation settings (task-146): the operating organisation's display
// name and an optional stylesheet that overrides the built-in brand styling.
//
// This is a SINGLETON row and therefore deliberately NOT a BaseEntity: it has a
// fixed SMALLINT id of 1 rather than a generated UUID, so the schema itself can
// make a second row unrepresentable. V18 seeds the row, so every read finds it
// and no code path needs a lazy create.
//
// The CHECK is declared here as well as in Flyway (V18) because %test/%dev build
// the schema from Hibernate with Flyway off — a constraint that lives only in
// the migration does not exist where the tests run (the V15 lesson).
@Entity
@Table(
    name = "system_settings",
    check = [CheckConstraint(name = "ck_system_settings_singleton", constraint = "id = 1")]
)
class SystemSettings {

    @Id
    @Column(name = "id", nullable = false)
    var id: Short = SINGLETON_ID

    // NULL means "no name configured" — the frontend then falls back to the
    // i18n `brand.name`. Blank input is normalised to NULL by the service.
    @Column(name = "display_name", length = 200)
    var displayName: String? = null

    @Column(name = "custom_css", columnDefinition = "TEXT")
    var customCss: String? = null

    @Column(name = "custom_css_filename", length = 255)
    var customCssFilename: String? = null

    @Column(name = "custom_css_updated_at")
    var customCssUpdatedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @Column(name = "updated_at")
    var updatedAt: Instant? = null

    @PrePersist
    fun onPrePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }

    companion object {
        const val SINGLETON_ID: Short = 1
    }
}
