package io.github.theestimator.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "projects")
class Project : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var name: String? = null

    var description: String? = null

    var client: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProjectStatus = ProjectStatus.ACTIVE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: User? = null

    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], orphanRemoval = true)
    var estimations: MutableList<Estimation> = mutableListOf()
}
