package com.metrolist.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Supplies the credentials that make a YouTube Music request act as the signed-in user. */
interface AuthProvider {
    val signedIn: StateFlow<Boolean>

    /** Headers to add to a request to [origin]. Empty when signed out. Computed per call (they expire). */
    fun headers(origin: String): Map<String, String>

    /** Forgets the credentials. */
    fun signOut()
}

/** Used when no account is configured: every request is anonymous. */
object NoAuth : AuthProvider {
    override val signedIn: StateFlow<Boolean> = MutableStateFlow(false)
    override fun headers(origin: String): Map<String, String> = emptyMap()
    override fun signOut() = Unit
}
