package io.github.theestimator.repository

import io.github.theestimator.domain.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, UUID> {

    fun findByEntraSubjectId(subjectId: String): User? =
        find("entraSubjectId", subjectId).firstResult()
}
