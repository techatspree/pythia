package io.github.theestimator.auth

interface AuthModule {
    fun name(): String
    fun isActive(): Boolean
}
