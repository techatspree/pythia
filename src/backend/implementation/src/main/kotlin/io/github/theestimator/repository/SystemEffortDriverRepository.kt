package io.github.theestimator.repository

import io.github.theestimator.domain.system.SystemEffortDriver
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SystemEffortDriverRepository : PanacheRepositoryBase<SystemEffortDriver, UUID> {

    fun findAllOrdered(): List<SystemEffortDriver> = list("ORDER BY position")
}
