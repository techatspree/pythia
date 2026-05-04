package io.github.theestimator.service

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.repository.EstimationRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class EstimationService(
    private val estimationRepository: EstimationRepository
) {

    fun findById(id: UUID): Estimation? = estimationRepository.findById(id)

    fun findByProjectId(projectId: UUID): List<Estimation> = estimationRepository.findByProjectId(projectId)

    @Transactional
    fun create(offer: String, project: Project, description: String? = null): Estimation {
        val estimation = Estimation().apply {
            this.offer = offer
            this.project = project
            this.description = description
        }
        estimationRepository.persist(estimation)
        return estimation
    }
}
