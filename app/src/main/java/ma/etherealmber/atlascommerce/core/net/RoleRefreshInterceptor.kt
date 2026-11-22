package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Handles the legacy v2 role-revision contract only when the Worker explicitly returns
 * HTTP 409 / ROLE_CHANGED. Native-pilot responses use their own contract and do not enter
 * this path unless they emit this exact legacy code.
 *
 * The rejected request is replayed at most once, with the server-issued token. A failed
 * refresh leaves the original response intact; only an actual 401 from role-refresh
 * invalidates the session.
 */
class RoleRefreshInterceptor(
    private val tokenHolder: TokenHolder,
    private val deviceFingerprint: () -> String,
    private val sessionEvents: SessionEvents,
    moshi: Moshi,
) : okhttp3.Interceptor {
    private val jsonMapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )
    private val refreshLock = Any()
    private val refreshClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val request = chain.request()
        val first = chain.proceed(request)
        if (first.code != 409 || request.url.encodedPath.endsWith("/api/auth/role-refresh")) return first
        val errorCode = runCatching {
            jsonMapAdapter.fromJson(first.peekBody(8_192L).string())?.get("code") as? String
        }.getOrNull()
        if (errorCode != "ROLE_CHANGED") return first

        val authorization = request.header("Authorization") ?: return first
        val oldToken = authorization.removePrefix("Bearer ").takeIf { it != authorization && it.isNotBlank() } ?: return first
        val currentToken = synchronized(refreshLock) {
            if (tokenHolder.token != oldToken) {
                tokenHolder.roleRefreshes.value
                    ?.takeIf { it.previousToken == oldToken && it.token == tokenHolder.token }
                    ?.token
            } else {
                refreshLegacyRole(request, authorization, oldToken)
            }
        } ?: return first

        first.close()
        val retry = request.newBuilder()
            .header("Authorization", "Bearer $currentToken")
            .build()
        return chain.proceed(retry)
    }

    /** Called under refreshLock, so concurrent 409s for one token share one rotation. */
    private fun refreshLegacyRole(request: Request, authorization: String, oldToken: String): String? {
        val fingerprint = runCatching(deviceFingerprint).getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val refreshUrl = request.url.newBuilder()
            .encodedPath("/api/auth/role-refresh")
            .query(null)
            .fragment(null)
            .build()
        val body = jsonMapAdapter.toJson(mapOf("fingerprint" to fingerprint)).toRequestBody(JSON_MEDIA_TYPE)
        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", authorization)
            .header("X-Atlas-Client", "android")
            .header("X-Device-Fingerprint", fingerprint)
            .post(body)
            .build()

        val response = try {
            refreshClient.newCall(refreshRequest).execute()
        } catch (_: Exception) {
            return null
        }
        response.use { result ->
            if (result.code == 401) {
                sessionEvents.notifyExpired(authorization, "Session invalide — reconnectez-vous.")
                return null
            }
            if (!result.isSuccessful) return null
            val payload = runCatching { jsonMapAdapter.fromJson(result.body?.string().orEmpty()) }.getOrNull() ?: return null
            val data = payload["data"] as? Map<*, *> ?: return null
            val rotatedToken = (data["token"] as? String)?.takeIf(String::isNotBlank) ?: return null
            val role = (data["role"] as? String)?.takeIf(String::isNotBlank) ?: return null
            return if (tokenHolder.installRoleRefresh(oldToken, rotatedToken, role)) rotatedToken else null
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
