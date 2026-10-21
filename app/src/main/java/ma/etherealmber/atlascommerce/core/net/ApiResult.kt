package ma.etherealmber.atlascommerce.core.net

/** Result of every network operation — the UI never sees raw exceptions. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Failure(
        val code: String,
        val message: String,
        val httpStatus: Int? = null,
    ) : ApiResult<Nothing>
}

fun <T> ApiResult<T>.successOrNull(): T? = (this as? ApiResult.Success)?.data

fun <T> ApiResult<T>.failureOrNull(): ApiResult.Failure? = this as? ApiResult.Failure

fun <T> ApiResult<T>.dataOr(fallback: T): T = successOrNull() ?: fallback

/** Envelope returned by the Atlas worker: `{ success, data, error, code, message }`. */
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
)

/** Shape shared by the auth endpoints (token is at the top level, not in `data`). */
interface ApiStatus {
    val success: Boolean
    val error: String?
    val code: String?
}

/** Error body used by [ApiRunner] to translate HTTP failures. */
data class ApiErrorPayload(
    val success: Boolean = false,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
)
