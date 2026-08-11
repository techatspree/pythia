package io.pythia.service

import io.pythia.domain.system.SystemSettings
import io.pythia.repository.SystemSettingsRepository
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional

// Guarantees the singleton system_settings row exists, in EVERY profile
// (task-146), mirroring the AuthProviderGuard / MethodRegistryBootstrap
// startup-observer pattern.
//
// V18 seeds the row for the Flyway-managed schemas (prod, dev-minikube), but
// %test and %dev build the schema from Hibernate with Flyway OFF — so there the
// migration's INSERT never runs and every read would fail. This is the V15
// migration/entity parity lesson in its DATA form: mirroring the CHECK
// constraint on the entity is not enough when the migration also seeds a row.
@ApplicationScoped
class SystemSettingsBootstrap(
    private val settingsRepository: SystemSettingsRepository
) {

    @Transactional
    @Suppress("UnusedParameter")
    fun onStart(@Observes event: StartupEvent) {
        if (settingsRepository.findById(SystemSettings.SINGLETON_ID) == null) {
            settingsRepository.persist(SystemSettings())
            Log.info("Created the singleton system_settings row (schema had none)")
        }
    }
}
