package ma.etherealmber.atlascommerce.core.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Authoritative role/token pair returned by the legacy role-refresh contract. */
data class RefreshedSessionRole(val previousToken: String, val token: String, val role: String)

/**
 * Holds the bearer token for the current session in memory only.
 *
 * The persistent copy is encrypted at rest (see CryptoStore); this holder exists
 * so the OkHttp interceptor can read the token without suspending and without a
 * circular dependency on the session layer.
 */
@Singleton
class TokenHolder @Inject constructor() {
    private val _roleRefreshes = MutableStateFlow<RefreshedSessionRole?>(null)
    val roleRefreshes = _roleRefreshes.asStateFlow()

    @Volatile
    var token: String? = null
        private set

    @Synchronized
    fun set(token: String?) {
        val previous = this.token
        this.token = token
        if (token != previous) _roleRefreshes.value = null
    }

    @Synchronized
    fun clear() {
        token = null
        _roleRefreshes.value = null
    }

    @Synchronized
    fun installRoleRefresh(expectedToken: String, refreshedToken: String, role: String): Boolean {
        if (token != expectedToken || refreshedToken.isBlank() || role.isBlank()) return false
        token = refreshedToken
        _roleRefreshes.tryEmit(RefreshedSessionRole(expectedToken, refreshedToken, role))
        return true
    }

    fun hasSession(): Boolean = !token.isNullOrBlank()
}
