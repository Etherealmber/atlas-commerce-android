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
    suspend fun getPersonalBest(serverUrl: String, bearerToken: String): ApiResult<Int> = runner.direct {
        apiProvider.apiForSession(serverUrl, bearerToken).getSnakePersonalBest()
            .personalBest.also(::requireValidScore)
    }

    suspend fun putPersonalBest(serverUrl: String, bearerToken: String, score: Int): ApiResult<Int> = runner.direct {
        requireValidScore(score)
        apiProvider.apiForSession(serverUrl, bearerToken)
            .putSnakePersonalBest(SnakePersonalBestDto(personalBest = score))
            .personalBest
            .also(::requireValidScore)
    }

    private fun requireValidScore(score: Int) {
        require(score >= 0) { "Snake personal best must be non-negative." }
    }
}
