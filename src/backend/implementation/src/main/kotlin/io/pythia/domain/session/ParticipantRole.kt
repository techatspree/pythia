package io.pythia.domain.session

/** A session participant is either the moderator who drives the flow or an
 *  estimator who votes. */
enum class ParticipantRole {
    MODERATOR,
    ESTIMATOR
}
