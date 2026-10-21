package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.model.LoginRequest
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import ma.etherealmber.atlascommerce.data.model.OtpRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiProvider: ApiProvider,
    private val runner: ApiRunner,
) {

    suspend fun login(username: String, password: String, fingerprint: String): ApiResult<LoginResponse> =
        runner.status {
            apiProvider.api().login(
                LoginRequest(
                    username = username,
                    password = password,
                    fingerprint = fingerprint,
                ),
            )
        }

    suspend fun verifyOtp(
        username: String,
        password: String,
        otp: String,
        fingerprint: String,
    ): ApiResult<LoginResponse> = runner.status {
        apiProvider.api().verifyOtp(
            OtpRequest(
                username = username,
                password = password,
                otp = otp,
                fingerprint = fingerprint,
            ),
        )
    }

    suspend fun me(): ApiResult<MeDto> = runner.envelope { apiProvider.api().me() }
}
