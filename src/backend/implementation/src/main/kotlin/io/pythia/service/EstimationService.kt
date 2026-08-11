package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.method.EstimationMethod
import io.pythia.repository.EstimationRepository
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
    fun create(
        offer: String,
        project: Project,
        description: String? = null,
        method: EstimationMethod = EstimationMethod.THREE_POINT_PERT
    ): Estimation {
        val estimation = Estimation().apply {
            this.offer = offer
            this.project = project
            this.description = description
            this.method = method
        }
        estimationRepository.persist(estimation)
        return estimation
    }
}
