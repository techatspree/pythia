package io.pythia.service

import io.pythia.domain.system.SystemSettings
import io.pythia.repository.SystemSettingsRepository
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.annotation.Priority
import jakarta.enterprise.event.Observes
import jakarta.interceptor.Interceptor
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
    // `@Priority` is REQUIRED since task-213: TestDataSeeder observes the same
    // StartupEvent at `@Priority(1)` and submits drafts, and submitting now READS
    // the hours per working day from this row. Without a lower number this ran
    // AFTER the seeder and the dev backend failed to boot with "system_settings
    // row 1 is missing" — the same trap MethodRegistryBootstrap documents.
    fun onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) event: StartupEvent) {
        if (settingsRepository.findById(SystemSettings.SINGLETON_ID) == null) {
            settingsRepository.persist(SystemSettings())
            Log.info("Created the singleton system_settings row (schema had none)")
        }
    }
}
