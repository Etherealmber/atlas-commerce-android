package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a network call and converts every outcome into an [ApiResult].
 *
 * Server messages are already written in French by the worker, so they are
 * passed through untouched; transport or domain issues explicitly remind users
 * to verify what they typed and check that their Atlas Commerce Web instance is online.
 */
@Singleton
class ApiRunner @Inject constructor(private val moshi: Moshi) {

    private val errorAdapter by lazy { moshi.adapter(ApiErrorPayload::class.java) }

    suspend fun <T> envelope(block: suspend () -> ApiEnvelope<T>): ApiResult<T> {
        return try {
            val envelope = block()
            val data = envelope.data
            if (envelope.success && data != null) {
                ApiResult.Success(data)
            } else {
                ApiResult.Failure(
                    code = envelope.code ?: CODE_SERVER,
                    message = envelope.error ?: envelope.message ?: "Réponse inattendue du serveur.",
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            failure(t)
        }
    }

    suspend fun <T : ApiStatus> status(block: suspend () -> T): ApiResult<T> {
        return try {
            val response = block()
            if (response.success) {
                ApiResult.Success(response)
            } else {
                ApiResult.Failure(
                    code = response.code ?: CODE_SERVER,
                    message = response.error ?: "Action refusée par le serveur.",
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            failure(t)
        }
    }

    /** Used when an endpoint returns `{ success: true }` with optional/empty `data`. */
    suspend fun <T> statusRaw(block: suspend () -> ApiEnvelope<T>): ApiResult<ApiEnvelope<T>> {
        return try {
            val envelope = block()
            if (envelope.success) {
                ApiResult.Success(envelope)
            } else {
                ApiResult.Failure(
                    code = envelope.code ?: CODE_SERVER,
                    message = envelope.error ?: envelope.message ?: "Action refusée par le serveur.",
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            failure(t)
        }
    }

    private fun failure(t: Throwable): ApiResult.Failure = when (t) {
        is HttpException -> httpFailure(t)
        is JsonEncodingException -> notAtlas()
        is JsonDataException -> notAtlas()
        is UnknownHostException -> ApiResult.Failure(
            CODE_NETWORK,
            "Impossible de joindre ce serveur. Vérifiez votre connexion puis réessayez.",
        )
        is SocketTimeoutException -> ApiResult.Failure(
            CODE_NETWORK,
            "La connexion prend trop de temps. Réessayez dans quelques instants.",
        )
        is IOException -> ApiResult.Failure(
            CODE_NETWORK,
            "La connexion a été interrompue. Vérifiez votre réseau puis réessayez.",
        )
        else -> ApiResult.Failure(
            CODE_UNKNOWN,
            "Une erreur inattendue est survenue. Réessayez.",
        )
    }

    private fun httpFailure(exception: HttpException): ApiResult.Failure {
        val status = exception.code()
        val raw = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        if (raw != null && (raw.contains("__cf_chl_opt") || raw.contains("Just a moment", ignoreCase = true))) {
            return ApiResult.Failure(
                code = "CF_CHALLENGE_$status",
                message = "Requête bloquée par la vérification anti-bot Cloudflare ($status). Vérifiez l'adresse du serveur ou autorisez /api/*.",
                httpStatus = status,
            )
        }
        val payload = raw
            ?.takeIf { it.isNotBlank() }
            ?.let { body -> runCatching { errorAdapter.fromJson(body) }.getOrNull() }
        if (status == 426 || payload?.code == "APP_UPDATE_REQUIRED") {
            return ApiResult.Failure(
                code = "APP_UPDATE_REQUIRED",
                message = "Une mise à jour est requise pour continuer. Téléchargez la dernière version.",
                httpStatus = status,
            )
        }
        val message = if (status in 500..599) {
            "Une erreur du service est survenue. Réessayez plus tard."
        } else {
            (payload?.error ?: payload?.message ?: defaultMessage(status)).take(240)
        }
        return ApiResult.Failure(
            code = payload?.code ?: "HTTP_$status",
            message = message,
            httpStatus = status,
        )
    }

    private fun notAtlas(): ApiResult.Failure = ApiResult.Failure(
        CODE_NOT_ATLAS,
        "Ce site ne répond pas comme une API Atlas Commerce. Vérifiez que l'URL saisie est exacte et qu'Atlas Commerce Web y est installé.",
    )

    private fun defaultMessage(status: Int): String = when (status) {
        400 -> "Saisie invalide — vérifiez que les informations tapées sont exactes."
        401 -> "Identifiant ou mot de passe incorrect — vérifiez attentivement votre saisie."
        403 -> "Accès non autorisé pour ce rôle ou cet appareil."
        404 -> "Élément ou service introuvable (404) — vérifiez que ce que vous avez tapé est correct et que votre serveur Atlas Commerce est à jour."
        429 -> "Limite temporaire atteinte — veuillez patienter quelques instants."
        in 500..599 -> "Service temporairement indisponible ($status) — vérifiez l'état de votre serveur Atlas Commerce Web."
        else -> "Erreur réseau ($status) — vérifiez votre saisie et votre connexion."
    }

    companion object {
        const val CODE_NETWORK = "NETWORK"
        const val CODE_NOT_ATLAS = "NOT_ATLAS"
        const val CODE_SERVER = "SERVER"
        const val CODE_UNKNOWN = "UNKNOWN"
    }
}
