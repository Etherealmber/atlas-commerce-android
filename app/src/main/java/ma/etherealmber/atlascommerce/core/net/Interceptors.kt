package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the headers the worker expects.
 *
 * `X-Atlas-Client: android` is what lets the worker skip Cloudflare Turnstile for
 * the native app (the app links its token to the device fingerprint instead), and
 * `X-Device-Fingerprint` is required by `/api/auth/me` and every authenticated
 * route because sessions are bound to the device.
 */
class AtlasHeaderInterceptor(
    private val tokenHolder: TokenHolder,
    private val deviceIdentity: DeviceIdentity,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .header("X-Device-Fingerprint", deviceIdentity.fingerprint)
            .header("User-Agent", deviceIdentity.userAgent)
            .apply {
                tokenHolder.token?.takeIf { it.isNotBlank() }?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()
        return chain.proceed(request)
    }
}

/**
 * Detects a dead session (401 with an auth code that is *not* a user mistake)
 * and raises a single app-wide event. Wrong-password / wrong-OTP replies never
 * destroy the session, matching the worker and the web client.
 */
class SessionGuardInterceptor(
    private val sessionEvents: SessionEvents,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && request.header("Authorization") != null) {
            val code = runCatching {
                response.peekBody(MAX_PEEK).string().let { body ->
                    CODE_REGEX.find(body)?.groupValues?.getOrNull(1)
                }
            }.getOrNull()
            if (code == null || code !in USER_MISTAKE_CODES) {
                sessionEvents.notifyExpired()
            }
        }
        return response
    }

    private companion object {
        const val MAX_PEEK = 4096L
        val CODE_REGEX = Regex("\"code\"\\s*:\\s*\"([A-Z0-9_]+)\"")
        val USER_MISTAKE_CODES = setOf(
            "BAD_PASSWORD",
            "PASSWORD_REQUIRED",
            "BAD_OTP",
            "OTP_REQUIRED",
            "OTP_EXPIRED",
            "OTP_NOT_REQUESTED",
            "RATE_LIMITED",
            "TURNSTILE_FAILED",
        )
    }
}
