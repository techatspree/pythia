package io.github.theestimator.domain

import jakarta.persistence.*
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
