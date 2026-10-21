package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a network call and converts every outcome into an [ApiResult].
 *
 * Server messages are already written in French by the worker, so they are
 * passed through untouched; only transport problems get generic wording.
 */
@Singleton
class ApiRunner @Inject constructor(moshi: Moshi) {

    private val errorAdapter = moshi.adapter(ApiErrorPayload::class.java)

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

    private fun failure(t: Throwable): ApiResult.Failure = when (t) {
        is HttpException -> httpFailure(t)
        is JsonEncodingException -> notAtlas()
        is JsonDataException -> notAtlas()
        is IOException -> ApiResult.Failure(
            CODE_NETWORK,
            "Impossible de joindre le serveur. Vérifiez l'adresse du serveur et votre connexion.",
        )
        else -> ApiResult.Failure(
            CODE_UNKNOWN,
            t.message?.takeIf { it.isNotBlank() } ?: "Erreur inattendue.",
        )
    }

    private fun httpFailure(exception: HttpException): ApiResult.Failure {
        val status = exception.code()
        val raw = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        val payload = raw
            ?.takeIf { it.isNotBlank() }
            ?.let { body -> runCatching { errorAdapter.fromJson(body) }.getOrNull() }
        val message = payload?.error
            ?: payload?.message
            ?: defaultMessage(status)
        return ApiResult.Failure(
            code = payload?.code ?: "HTTP_$status",
            message = message,
            httpStatus = status,
        )
    }

    private fun notAtlas(): ApiResult.Failure = ApiResult.Failure(
        CODE_NOT_ATLAS,
        "Ce serveur ne répond pas comme une API Atlas. Vérifiez l'adresse (elle doit pointer vers votre worker Atlas).",
    )

    private fun defaultMessage(status: Int): String = when (status) {
        400 -> "Requête invalide."
        401 -> "Identifiant ou mot de passe incorrect."
        403 -> "Accès refusé."
        404 -> "Serveur introuvable — vérifiez l'adresse."
        429 -> "Trop de tentatives — réessayez dans une minute."
        in 500..599 -> "Erreur serveur ($status). Réessayez plus tard."
        else -> "Erreur réseau ($status)."
    }

    companion object {
        const val CODE_NETWORK = "NETWORK"
        const val CODE_NOT_ATLAS = "NOT_ATLAS"
        const val CODE_SERVER = "SERVER"
        const val CODE_UNKNOWN = "UNKNOWN"
    }
}
