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
    suspend fun getPersonalBest(): ApiResult<Int> = runner.direct {
        apiProvider.api().getSnakePersonalBest().personalBest.also(::requireValidScore)
    }

    suspend fun putPersonalBest(score: Int): ApiResult<Int> = runner.direct {
        requireValidScore(score)
        apiProvider.api().putSnakePersonalBest(SnakePersonalBestDto(personalBest = score))
            .personalBest
            .also(::requireValidScore)
    }

    private fun requireValidScore(score: Int) {
        require(score >= 0) { "Snake personal best must be non-negative." }
    }
}
