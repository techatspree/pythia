package io.pythia.repository

import io.pythia.domain.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, UUID> {

    fun findByEntraSubjectId(subjectId: String): User? =
        find("entraSubjectId", subjectId).firstResult()

    // Every provisioned user, for the project-owner picker (task-200).
    fun listByDisplayName(): List<User> = listAll(Sort.by("displayName"))
}
