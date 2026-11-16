package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.model.SnakePersonalBestDto
import javax.inject.Inject
import javax.inject.Singleton

/** Private, authenticated personal-best API. The API provider supplies the active session and device headers. */
@Singleton
class SnakeScoreRepository @Inject constructor(
    private val apiProvider: ApiProvider,
    private val runner: ApiRunner,
) {
    suspend fun getPersonalBest(serverUrl: String, bearerToken: String): ApiResult<Int> {
        val api = apiProvider.apiForSession(serverUrl) ?: return sessionChangedFailure()
        val authorization = bearerToken.asBearerAuthorization() ?: return missingTokenFailure()
        return runner.direct {
            api.getSnakePersonalBest(authorization).personalBest.also(::requireValidScore)
        }
    }

    suspend fun putPersonalBest(serverUrl: String, bearerToken: String, score: Int): ApiResult<Int> {
        requireValidScore(score)
        val api = apiProvider.apiForSession(serverUrl) ?: return sessionChangedFailure()
        val authorization = bearerToken.asBearerAuthorization() ?: return missingTokenFailure()
        return runner.direct {
            api.putSnakePersonalBest(authorization, SnakePersonalBestDto(personalBest = score))
                .personalBest
                .also(::requireValidScore)
        }
    }

    private fun String.asBearerAuthorization(): String? = takeIf(String::isNotBlank)?.let { "Bearer $it" }

    private fun sessionChangedFailure() = ApiResult.Failure(
        code = "SNAKE_SESSION_CHANGED",
        message = "The active server changed before the Snake score could be synced.",
    )

    private fun missingTokenFailure() = ApiResult.Failure(
        code = "SNAKE_SESSION_MISSING",
        message = "A verified session is required to sync the Snake score.",
    )

    private fun requireValidScore(score: Int) {
        require(score >= 0) { "Snake personal best must be non-negative." }
    }
}
