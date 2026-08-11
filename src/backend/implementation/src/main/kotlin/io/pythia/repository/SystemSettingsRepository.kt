package io.pythia.repository

import io.pythia.domain.system.SystemSettings
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SystemSettingsRepository : PanacheRepositoryBase<SystemSettings, Short> {

    // The singleton row. V18 seeds it and the CHECK (id = 1) keeps it the only
    // one, so this never returns null in a migrated schema; `error(...)` makes a
    // missing row a loud bug rather than a silent default.
    fun get(): SystemSettings =
        findById(SystemSettings.SINGLETON_ID)
            ?: error("system_settings row ${SystemSettings.SINGLETON_ID} is missing — was V18 applied?")
}
