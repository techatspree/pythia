package io.pythia.auth

interface AuthModule {
    fun name(): String
    fun isActive(): Boolean
}
