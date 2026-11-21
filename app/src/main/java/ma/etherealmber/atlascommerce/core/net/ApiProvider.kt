package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.Moshi
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/** True when an app-wide interceptor must be excluded from startup resume preparation. */
internal fun isStartupResumeExcludedInterceptor(interceptor: Interceptor): Boolean =
    interceptor is AtlasHeaderInterceptor || interceptor is SessionGuardInterceptor

/**
 * Builds the Retrofit client for the currently selected server.
 *
 * Only one Retrofit instance is kept alive; it is rebuilt (lazily, on first use)
 * when the user switches server, which is why [ServerConfig] is read at call
 * time instead of at injection time.
 */
@Singleton
class ApiProvider @Inject constructor(
    private val serverConfig: ServerConfig,
    private val moshi: Moshi,
    private val client: OkHttpClient,
    private val deviceIdentity: DeviceIdentity,
    private val tokenHolder: TokenHolder,
) {

    private val lock = Any()
    private var builtFor: String? = null
    private var builtApi: AtlasApi? = null

    fun baseUrl(): String = ServerUrl.withSlash(serverConfig.baseUrl())

    fun api(): AtlasApi = synchronized(lock) {
        apiForBaseUrl(baseUrl())
    }

    /** Return an API bound to the currently selected server only when it matches the session origin. */
    fun apiForSession(serverUrl: String): AtlasApi? {
        val expectedBase = ServerUrl.normalize(serverUrl)
            ?.let { ServerUrl.withSlash(it) }
            ?: return null
        return synchronized(lock) {
            val selectedBase = baseUrl()
            if (selectedBase != expectedBase) return@synchronized null
            apiForBaseUrl(selectedBase)
        }
    }

    /** A one-purpose client pinned to the selected HTTPS origin; redirects are never followed. */
    fun desktopPhoneApiForOrigin(origin: String, expectedBearer: String): AtlasApi? {
        if (expectedBearer.isBlank() || tokenHolder.token != expectedBearer) return null
        val selected = ServerUrl.normalize(serverConfig.baseUrl()) ?: return null
        val selectedUri = runCatching { java.net.URI(selected) }.getOrNull() ?: return null
        if (!selectedUri.scheme.equals("https", true) || selectedUri.rawPath.orEmpty().let { it.isNotEmpty() && it != "/" } ||
            selectedUri.rawQuery != null || selectedUri.rawFragment != null || selectedUri.rawUserInfo != null
        ) return null
        val host = selectedUri.host ?: return null
        val authority = selectedUri.rawAuthority ?: return null
        val explicitPort = if (host.startsWith("[")) authority.substringAfter(']', "").startsWith(":")
            else authority.substring(host.length).startsWith(":")
        if (explicitPort && selectedUri.port !in 1..65535) return null
        val selectedOrigin = "https://${host.lowercase()}" + if (explicitPort) ":${selectedUri.port}" else ""
        if (origin != selectedOrigin) return null
        val pinnedClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .apply {
                interceptors().add(0, DesktopOriginGuardInterceptor(serverConfig, selectedOrigin, tokenHolder, expectedBearer))
                interceptors().add(1, DesktopDecisionOneShotInterceptor())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("$selectedOrigin/")
            .client(pinnedClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AtlasApi::class.java)
    }

    private fun apiForBaseUrl(base: String): AtlasApi {
        val cached = builtApi
        if (cached != null && builtFor == base) return cached
        val api = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AtlasApi::class.java)
        builtFor = base
        builtApi = api
        return api
    }

    /**
     * Creates a one-shot API client for verifying a saved session during process startup.
     * It omits the global token holder and session guard, and sends only the supplied token
     * to the normalized server origin selected by the user.
     */
    fun startupResumeApi(serverUrl: String, bearerToken: String): AtlasApi {
        val normalized = ServerUrl.normalize(serverUrl)
            ?: throw IllegalArgumentException("Invalid selected server URL")
        require(bearerToken.isNotBlank()) { "Missing saved session token" }
        val isolatedClient = client.newBuilder().apply {
            interceptors().removeAll(::isStartupResumeExcludedInterceptor)
            // This client only sends the idempotent GET /api/auth/me; the regular API client
            // keeps retries disabled so mutating POSTs are never replayed implicitly.
            retryOnConnectionFailure(true)
            addInterceptor(PinnedSessionHeadersInterceptor(deviceIdentity, bearerToken))
        }.build()
        return Retrofit.Builder()
            .baseUrl(ServerUrl.withSlash(normalized))
            .client(isolatedClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AtlasApi::class.java)
    }

    /** Force a new Retrofit instance after a server change. */
    fun invalidate() {
        synchronized(lock) {
            builtApi = null
            builtFor = null
        }
    }
}

private class DesktopOriginGuardInterceptor(
    private val serverConfig: ServerConfig,
    private val expectedOrigin: String,
    private val tokenHolder: TokenHolder,
    private val expectedBearer: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val selected = ServerUrl.normalize(serverConfig.baseUrl())
            ?: throw java.io.IOException("Desktop approval server selection is invalid")
        val uri = runCatching { java.net.URI(selected) }.getOrNull()
            ?: throw java.io.IOException("Desktop approval server selection is invalid")
        val host = uri.host ?: throw java.io.IOException("Desktop approval server selection is invalid")
        val authority = uri.rawAuthority ?: throw java.io.IOException("Desktop approval server selection is invalid")
        val explicitPort = if (host.startsWith("[")) authority.substringAfter(']', "").startsWith(":")
            else authority.substring(host.length).startsWith(":")
        val currentOrigin = if (uri.scheme.equals("https", true)) {
            "https://${host.lowercase()}" + if (explicitPort && uri.port in 1..65535) ":${uri.port}" else ""
        } else ""
        val expectedUrl = expectedOrigin.toHttpUrlOrNull()
            ?: throw java.io.IOException("Desktop approval origin is invalid")
        val requestUrl = chain.request().url
        if (tokenHolder.token != expectedBearer || currentOrigin != expectedOrigin || requestUrl.scheme != "https" ||
            requestUrl.host != expectedUrl.host || requestUrl.port != expectedUrl.port
        ) throw java.io.IOException("Desktop approval origin changed; request blocked")
        val response = chain.proceed(chain.request())
        val finalUrl = response.request.url
        if (finalUrl.scheme != "https" || finalUrl.host != expectedUrl.host ||
            finalUrl.port != expectedUrl.port
        ) {
            response.close()
            throw java.io.IOException("Desktop approval response origin mismatch")
        }
        return response
    }
}

private class PinnedSessionHeadersInterceptor(
    private val deviceIdentity: DeviceIdentity,
    private val bearerToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .addAtlasAppVersionHeaders()
            .header("X-Device-Fingerprint", deviceIdentity.fingerprintForRequest())
            .header("User-Agent", deviceIdentity.userAgent)
            .header("Authorization", "Bearer $bearerToken")
            .build()
        return chain.proceed(request)
    }
}
