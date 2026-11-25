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

    /** Returns whether the Worker actually marked the current device resumable. */
    suspend fun logout(): ApiResult<ActionAckDto> =
        runner.status { apiProvider.api().logout() }

    /** Verify a saved session and handle only the legacy v2 409 / ROLE_CHANGED contract. */
    suspend fun validateStartupSession(
        serverUrl: String,
        savedBearerToken: String,
        fingerprint: String,
    ): ApiResult<MeDto> {
        val first = runner.envelope { apiProvider.startupResumeApi(serverUrl, savedBearerToken).me() }
        val roleChanged = first as? ApiResult.Failure
        // Native-pilot sessions have a distinct contract and are not refreshed with the v2 endpoint.
        if (savedBearerToken.startsWith("np1.") || roleChanged == null || roleChanged.httpStatus != 409 || roleChanged.code != "ROLE_CHANGED") return first

        val refresh = runner.envelope {
            apiProvider.startupResumeApi(serverUrl, savedBearerToken)
                .refreshLegacyRole(ma.etherealmber.atlascommerce.data.model.StartupRoleRefreshRequest(fingerprint))
        }
        val rotated = when (refresh) {
            is ApiResult.Success -> refresh.data
            is ApiResult.Failure -> return refresh
        }
        if (rotated.token.isBlank() || rotated.role.isBlank()) {
            return ApiResult.Failure("ROLE_REFRESH_INVALID", "Réponse de mise à jour du rôle invalide.")
        }
        val retried = runner.envelope { apiProvider.startupResumeApi(serverUrl, rotated.token).me() }
        return when (retried) {
            is ApiResult.Success -> ApiResult.Success(
                retried.data.copy(token = rotated.token, role = rotated.role, startupRoleRefreshValidated = true),
            )
            is ApiResult.Failure -> retried
        }
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
        jobTitle: String? = null,
        workingHours: String? = null,
        timezone: String? = null,
    ): ApiResult<MeDto> = runner.envelope {
        apiProvider.api().updateMe(
            ProfileUpdateRequest(
                displayName = displayName,
                email = email,
                phone = phone,
                avatarUrl = avatarUrl,
                jobTitle = jobTitle,
                workingHours = workingHours,
                timezone = timezone,
            ),
        )
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

    suspend fun desktopPhonePrepare(
        origin: String,
        requestId: String,
        bearerToken: String,
    ): ApiResult<ma.etherealmber.atlascommerce.data.model.DesktopPhonePrepareDto> {
        val api = apiProvider.desktopPhoneApiForOrigin(origin, bearerToken)
            ?: return ApiResult.Failure("ORIGIN_MISMATCH", "Le serveur sélectionné a changé ou n'utilise pas cette origine HTTPS.")
        return runner.envelope {
            api.desktopPhonePrepare(
                "Bearer $bearerToken",
                ma.etherealmber.atlascommerce.data.model.DesktopPhonePrepareRequest(origin, requestId),
            )
        }
    }

    suspend fun desktopPhoneDecision(
        origin: String,
        requestId: String,
        nonce: String,
        decision: String,
        bearerToken: String,
    ): ApiResult<ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionDto> {
        require(decision == "approve" || decision == "deny")
        val api = apiProvider.desktopPhoneApiForOrigin(origin, bearerToken)
            ?: return ApiResult.Failure("ORIGIN_MISMATCH", "Le serveur sélectionné a changé ou n'utilise pas cette origine HTTPS.")
        return runner.envelope {
            api.desktopPhoneDecision(
                "Bearer $bearerToken",
                ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionRequest(
                    origin, requestId, nonce, decision, compareConfirmed = true,
                ),
            )
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
