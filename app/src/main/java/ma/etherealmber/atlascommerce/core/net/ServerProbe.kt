package ma.etherealmber.atlascommerce.core.net

import android.os.SystemClock
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.di.ProbeClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

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
 * Uses `/api/health` (public, unauthenticated) and surfaces clear guidance whenever
 * a domain contains a typo or when the linked Atlas Commerce web server is not deployed yet.
 */
@Singleton
class ServerProbe @Inject constructor(
    @ProbeClient private val client: OkHttpClient,
    private val moshi: Moshi,
) {

    private val healthAdapter by lazy {
        val healthType = Types.newParameterizedType(ApiEnvelope::class.java, HealthDto::class.java)
        moshi.adapter<ApiEnvelope<HealthDto>>(healthType)
    }

    suspend fun probe(rawUrl: String): ProbeReport = withContext(Dispatchers.IO) {
        probeBlocking(rawUrl)
    }

    private fun probeBlocking(rawUrl: String): ProbeReport {
        val base = ServerUrl.normalize(rawUrl)
            ?: return ProbeReport(
                ok = false,
                baseUrl = rawUrl,
                message = "Adresse invalide — vérifiez attentivement que ce que vous avez tapé est correct (ex. https://votre-domaine.com).",
            )
        if (ServerUrl.isCleartext(base) && !BuildConfig.ALLOW_CLEARTEXT) {
            return ProbeReport(
                ok = false,
                baseUrl = base,
                message = "HTTP n'est pas autorisé dans cette version. Vérifiez que l'adresse commence bien par https://.",
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
                    val isCfChallenge = body.contains("__cf_chl_opt") || body.contains("Just a moment", ignoreCase = true)
                    return@use ProbeReport(
                        ok = false,
                        baseUrl = base,
                        latencyMs = elapsed,
                        message = when {
                            isCfChallenge ->
                                "Accès bloqué par la protection anti-bot Cloudflare (${response.code}). Autorisez /api/* dans votre pare-feu ou vérifiez le domaine saisi."
                            response.code == 404 ->
                                "Aucune API Atlas trouvée à cette adresse (404). Vérifiez que l'URL saisie est exacte et qu'Atlas Commerce Web y est bien déployé."
                            response.code == 429 ->
                                "Serveur joignable mais temporairement limité (429). Patientez quelques instants puis réessayez."
                            response.code in 500..599 ->
                                "Site temporairement indisponible (${response.code}). Vérifiez votre déploiement Atlas Commerce Web."
                            else ->
                                "Connexion refusée (${response.code}). Vérifiez que l'adresse saisie est parfaitement exacte."
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
                        message = "Serveur Atlas Commerce vérifié · ${elapsed} ms",
                    )
                } else {
                    ProbeReport(
                        ok = false,
                        baseUrl = base,
                        latencyMs = elapsed,
                        message = "Ce site ne répond pas comme un espace Atlas Commerce. Vérifiez l'orthographe exacte de l'adresse saisie et assurez-vous d'avoir installé Atlas Commerce Web d'abord.",
                    )
                }
            }
        } catch (_: UnknownHostException) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = "Site introuvable — vérifiez bien que l'adresse tapée est exacte (sans faute de frappe) et que votre connexion Internet est active.",
            )
        } catch (_: SocketTimeoutException) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = "Délai d'attente dépassé — vérifiez que l'adresse saisie est correcte et que votre site Atlas Commerce est bien en ligne.",
            )
        } catch (_: SSLException) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = "Certificat HTTPS invalide — vérifiez l'exactitude du nom de domaine saisi.",
            )
        } catch (_: IOException) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = "Site ou serveur indisponible. Vérifiez que l'adresse tapée est exacte et que votre serveur Atlas Commerce Web fonctionne.",
            )
        } catch (t: Throwable) {
            ProbeReport(
                ok = false,
                baseUrl = base,
                message = t.message ?: "Vérification impossible — vérifiez votre saisie.",
            )
        }
    }
}
