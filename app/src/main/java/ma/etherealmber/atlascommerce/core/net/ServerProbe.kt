package ma.etherealmber.atlascommerce.core.net

import android.os.SystemClock
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.data.model.HealthDto
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class ProbeReport(
    val ok: Boolean,
    val baseUrl: String,
    val latencyMs: Long = 0,
    val environment: String? = null,
    val database: Boolean = false,
    val storage: Boolean = false,
    val message: String,
)

/**
 * Checks that an address really is an Atlas worker before we store it.
 *
 * Uses `/api/health` (public, unauthenticated) and reports latency so the user
 * can immediately see whether a self-hosted server is reachable.
 */
@Singleton
class ServerProbe @Inject constructor(
    private val client: OkHttpClient,
    moshi: Moshi,
) {

    private val healthType = Types.newParameterizedType(ApiEnvelope::class.java, HealthDto::class.java)
    private val healthAdapter = moshi.adapter<ApiEnvelope<HealthDto>>(healthType)

    suspend fun probe(rawUrl: String): ProbeReport = withContext(Dispatchers.IO) {
        probeBlocking(rawUrl)
    }

    private fun probeBlocking(rawUrl: String): ProbeReport {
        val base = ServerUrl.normalize(rawUrl)
            ?: return ProbeReport(false, rawUrl, message = "Adresse invalide.")
        if (ServerUrl.isCleartext(base) && !BuildConfig.ALLOW_CLEARTEXT) {
            return ProbeReport(
                ok = false,
                baseUrl = base,
                message = "HTTP n'est pas autorisé dans cette version. Utilisez une adresse HTTPS.",
            )
        }

        val request = Request.Builder()
            .url(ServerUrl.withSlash(base) + "api/health")
            .get()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .build()

        val started = SystemClock.elapsedRealtime()
        return try {
            client.newCall(request).execute().use { response ->
                val elapsed = SystemClock.elapsedRealtime() - started
                val body = runCatching { response.body?.string() }.getOrNull().orEmpty()
                if (!response.isSuccessful) {
                    return@use ProbeReport(
                        ok = false,
                        baseUrl = base,
                        latencyMs = elapsed,
                        message = when (response.code) {
                            404 -> "Aucune API Atlas à cette adresse (404)."
                            429 -> "Serveur joignable mais limité (429). Réessayez."
                            in 500..599 -> "Serveur en erreur (${response.code})."
                            else -> "Le serveur a refusé la vérification (${response.code})."
                        },
                    )
                }
                val parsed = runCatching { healthAdapter.fromJson(body) }.getOrNull()
                val health = parsed?.data
                if (parsed?.success == true && health?.ok == true) {
                    ProbeReport(
                        ok = true,
                        baseUrl = base,
                        latencyMs = elapsed,
                        environment = health.environment,
                        database = health.d1,
                        storage = health.r2,
                        message = "Serveur Atlas connecté · ${elapsed} ms",
                    )
                } else {
                    ProbeReport(
                        ok = false,
                        baseUrl = base,
                        latencyMs = elapsed,
                        message = "Réponse inattendue : cette adresse ne semble pas être un serveur Atlas.",
                    )
                }
            }
        } catch (e: IOException) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = "Serveur injoignable. Vérifiez l'adresse, le port et votre connexion.",
            )
        } catch (t: Throwable) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = t.message ?: "Vérification impossible.",
            )
        }
    }
}
