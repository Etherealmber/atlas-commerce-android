package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.model.ActionAckDto
import ma.etherealmber.atlascommerce.data.model.LoginRequest
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import ma.etherealmber.atlascommerce.data.model.OtpRequest
import ma.etherealmber.atlascommerce.data.model.ProfileUpdateRequest
import ma.etherealmber.atlascommerce.data.model.QrClaimRequest
import ma.etherealmber.atlascommerce.data.model.QrCompleteDto
import ma.etherealmber.atlascommerce.data.model.QrCompleteRequest
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.RevokeDeviceRequest
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.model.UserReportRequest
import ma.etherealmber.atlascommerce.data.model.UserReportReceiptDto
import ma.etherealmber.atlascommerce.data.model.VerifyLinkRequest
import javax.inject.Inject
import javax.inject.Singleton

internal fun requireResumePreparationConfirmation(result: ApiResult<ActionAckDto>): ApiResult<Unit> =
    when (result) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> if (result.data.success && result.data.resumePrepared) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Failure(
                code = result.data.code?.takeIf { it.isNotBlank() } ?: "RESUME_NOT_PREPARED",
                message = result.data.error?.takeIf { it.isNotBlank() }
                    ?: "The server did not confirm startup resume preparation.",
            )
        }
    }

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

    suspend fun logout(): ApiResult<Unit> {
        val result = runner.status { apiProvider.api().logout() }
        return when (result) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    /**
     * Prepare a same-device password-only return at process startup using the existing logout
     * route. This uses a server-pinned isolated client and never touches the global TokenHolder.
     */
    suspend fun prepareStartupResume(serverUrl: String, previousSessionToken: String): ApiResult<Unit> {
        if (previousSessionToken.isBlank()) return ApiResult.Failure("NO_SESSION", "No previous session")
        val result = runner.status {
            apiProvider.startupResumeApi(serverUrl).logout("Bearer $previousSessionToken")
        }
        return requireResumePreparationConfirmation(result)
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

    suspend fun verifyLink(
        username: String,
        otp: String,
        sig: String,
        exp: Long,
        fingerprint: String,
    ): ApiResult<LoginResponse> = runner.status {
        apiProvider.api().verifyLink(
            VerifyLinkRequest(
                username = username,
                otp = otp,
                sig = sig,
                exp = exp,
                fingerprint = fingerprint,
            ),
        )
    }

    suspend fun submitUserReport(request: UserReportRequest): ApiResult<UserReportReceiptDto> =
        when (val result = runner.statusRaw { apiProvider.api().submitUserReport(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                result.data.data ?: UserReportReceiptDto(),
            )
        }

    suspend fun me(): ApiResult<MeDto> = runner.envelope { apiProvider.api().me() }

    suspend fun updateProfile(
        displayName: String? = null,
        email: String? = null,
        phone: String? = null,
        avatarUrl: String? = null,
    ): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().updateMe(
                ProfileUpdateRequest(
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    avatarUrl = avatarUrl,
                ),
            )
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun devices(): ApiResult<List<TrustedDeviceDto>> =
        runner.envelope { apiProvider.api().devices() }

    suspend fun revokeDevice(fingerprint: String): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().revokeDevice(RevokeDeviceRequest(fingerprint = fingerprint))
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun qrStatus(token: String): ApiResult<QrSessionStatusDto> =
        runner.envelope { apiProvider.api().qrStatus(token) }

    suspend fun qrClaim(
        token: String,
        fingerprint: String,
        userAgent: String,
    ): ApiResult<QrSessionStatusDto> = runner.envelope {
        apiProvider.api().qrClaim(
            token = token,
            body = QrClaimRequest(fingerprint = fingerprint, userAgent = userAgent),
        )
    }

    suspend fun qrPhoneBind(
        token: String,
        fingerprint: String,
        userAgent: String,
        confirm: Boolean,
    ): ApiResult<QrSessionStatusDto> = runner.envelope {
        apiProvider.api().qrPhoneBind(
            token = token,
            body = QrClaimRequest(fingerprint = fingerprint, userAgent = userAgent, confirm = confirm),
        )
    }

    suspend fun qrDeny(token: String): ApiResult<QrSessionStatusDto> =
        runner.envelope { apiProvider.api().qrDeny(token) }

    suspend fun qrComplete(token: String, fingerprint: String): ApiResult<QrCompleteDto> =
        runner.envelope {
            apiProvider.api().qrComplete(token, QrCompleteRequest(fingerprint = fingerprint))
        }
}
