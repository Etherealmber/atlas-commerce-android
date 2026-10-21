package ma.etherealmber.atlascommerce.core.net

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the bearer token for the current session in memory only.
 *
 * The persistent copy is encrypted at rest (see CryptoStore); this holder exists
 * so the OkHttp interceptor can read the token without suspending and without a
 * circular dependency on the session layer.
 */
@Singleton
class TokenHolder @Inject constructor() {
    @Volatile
    var token: String? = null
        private set

    fun set(token: String?) {
        this.token = token
    }

    fun clear() {
        token = null
    }

    fun hasSession(): Boolean = !token.isNullOrBlank()
}
