package io.github.theestimator.method

import io.github.theestimator.StandardMethods
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.interceptor.Interceptor

/**
 * Installs the domain's standard estimation methods into
 * [EstimationMethodRegistry] at boot.
 *
 * Required since task-143: the registry used to self-populate from an `init {}`
 * block, but each method is now its own Gradle module and `:domain:core` (which
 * owns the registry) is forbidden to reference them. Installation therefore
 * moved to the aggregator's [StandardMethods], and each runtime bootstraps it —
 * for the backend, here. Without this every `require(...)` / `requireSession(...)`
 * would throw on the first export, calculation or session reduction.
 *
 * Mirrors `AuthProviderGuard`'s `StartupEvent` shape.
 */
@ApplicationScoped
class MethodRegistryBootstrap {

    // `@Priority` is REQUIRED, not decoration, and the VALUE matters: CDI leaves
    // observer order unspecified otherwise, and `TestDataSeeder` also observes
    // StartupEvent — at `@Priority(1)` — where it seeds a draft and submits it,
    // which calls EstimationVersion.calculate() and so needs the registry
    // already populated. This must therefore run at a LOWER number than 1;
    // PLATFORM_BEFORE (0) is the smallest standard constant. Anything higher
    // (LIBRARY_BEFORE = 1000, the default APPLICATION = 2000) boots the dev
    // backend straight into "No estimation method module registered for
    // THREE_POINT_PERT". If another observer ever needs to precede this one,
    // it cannot — install the methods from there instead.
    //
    // The StartupEvent parameter is how CDI selects the observer; deliberately unread.
    @Suppress("UnusedParameter")
    fun onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) event: StartupEvent) {
        StandardMethods.installAll()
        Log.info("Installed estimation methods: ${EstimationMethodRegistry.all().map { it.method }}")
    }
}
