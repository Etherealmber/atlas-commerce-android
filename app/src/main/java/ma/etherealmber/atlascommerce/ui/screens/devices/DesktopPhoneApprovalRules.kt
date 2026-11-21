package ma.etherealmber.atlascommerce.ui.screens.devices

import java.time.Instant

/** Pure fail-closed rules shared by the v2 approval UI and its contract tests. */
internal class SingleUseDecisionGate {
    private var attempted = false

    @Synchronized
    fun begin(
        comparisonConfirmed: Boolean,
        requestExpiresAt: String,
        consentExpiresAt: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (attempted || !DesktopPhoneApprovalRules.canDecide(
                comparisonConfirmed, requestExpiresAt, consentExpiresAt, nowEpochMillis,
            )
        ) return false
        attempted = true
        return true
    }
}

internal object DesktopPhoneApprovalRules {
    fun canDecide(
        comparisonConfirmed: Boolean,
        requestExpiresAt: String,
        consentExpiresAt: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean = comparisonConfirmed && isValidWindow(requestExpiresAt, consentExpiresAt, nowEpochMillis)

    fun isValidWindow(requestExpiresAt: String, consentExpiresAt: String, nowEpochMillis: Long): Boolean = runCatching {
        val request = Instant.parse(requestExpiresAt).toEpochMilli()
        val consent = Instant.parse(consentExpiresAt).toEpochMilli()
        consent > nowEpochMillis && request > nowEpochMillis && consent <= request && consent - nowEpochMillis <= 120_000L
    }.getOrDefault(false)

    fun matchesResponse(approve: Boolean, status: String): Boolean =
        status == if (approve) "approved" else "denied"

}
