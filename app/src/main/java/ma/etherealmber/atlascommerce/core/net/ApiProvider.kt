package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.Moshi
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

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

    /** Force a new Retrofit instance after a server change. */
    fun invalidate() {
        synchronized(lock) {
            builtApi = null
            builtFor = null
        }
    }
}
