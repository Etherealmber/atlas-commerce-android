package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.Moshi
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
) {

    private val lock = Any()
    private var builtFor: String? = null
    private var builtApi: AtlasApi? = null

    fun baseUrl(): String = ServerUrl.withSlash(serverConfig.baseUrl())

    fun api(): AtlasApi {
        val base = baseUrl()
        synchronized(lock) {
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
    }

    /**
     * Creates an uncached API client pinned to one verified session origin and token.
     * Mutating calls made through this API cannot inherit a later account's global token.
     */
    fun apiForSession(serverUrl: String, bearerToken: String): AtlasApi {
        val normalized = ServerUrl.normalize(serverUrl)
            ?: throw IllegalArgumentException("Invalid session server URL")
        require(bearerToken.isNotBlank()) { "Missing session token" }
        val pinnedHeaders = PinnedSessionHeadersInterceptor(deviceIdentity, bearerToken)
        val sessionClient = client.newBuilder().apply {
            val appInterceptors = interceptors()
            val authHeaderIndex = appInterceptors.indexOfFirst { it is AtlasHeaderInterceptor }
            if (authHeaderIndex >= 0) {
                appInterceptors[authHeaderIndex] = pinnedHeaders
            } else {
                appInterceptors.add(0, pinnedHeaders)
            }
        }.build()
        return Retrofit.Builder()
            .baseUrl(ServerUrl.withSlash(normalized))
            .client(sessionClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AtlasApi::class.java)
    }

    /**
     * Creates a one-shot API client for a pinned session credential. Startup uses it to
     * verify a saved account; session-bound score requests use it to avoid following a
     * later global token/server switch. It sends only the supplied token to the selected
     * normalized server origin.
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
