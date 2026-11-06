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
     * Creates a one-shot client for startup resume preparation. It reuses the existing
     * /api/auth/logout route, but deliberately omits the global token and session-guard
     * interceptors so an old token is never installed in TokenHolder or reported as a
     * newly expired active session while the app is still initializing.
     */
    fun startupResumeApi(serverUrl: String): AtlasApi {
        val normalized = ServerUrl.normalize(serverUrl)
            ?: throw IllegalArgumentException("Invalid selected server URL")
        val isolatedClient = client.newBuilder().apply {
            interceptors().removeAll(::isStartupResumeExcludedInterceptor)
            addInterceptor(StartupResumeHeadersInterceptor(deviceIdentity))
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

private class StartupResumeHeadersInterceptor(
    private val deviceIdentity: DeviceIdentity,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .addAtlasAppVersionHeaders()
            .header("X-Device-Fingerprint", deviceIdentity.fingerprint)
            .header("User-Agent", deviceIdentity.userAgent)
            .build()
        return chain.proceed(request)
    }
}
