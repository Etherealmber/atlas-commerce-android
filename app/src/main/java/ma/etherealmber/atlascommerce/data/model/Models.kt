package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import ma.etherealmber.atlascommerce.core.net.ApiStatus
import kotlin.math.ceil

// ---------------------------------------------------------------------------
// Health / server discovery / self-update
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ActionAckDto(
    override val success: Boolean = false,
    override val error: String? = null,
    override val code: String? = null,
) : ApiStatus

@JsonClass(generateAdapter = true)
data class HealthDto(
    val ok: Boolean = false,
    val d1: Boolean = false,
    val kv: Boolean = false,
    val r2: Boolean = false,
    val cdn: String? = null,
    val environment: String? = null,
    val time: String? = null,
)

@JsonClass(generateAdapter = true)
data class AndroidUpdateDto(
    override val success: Boolean = false,
    val ok: Boolean = false,
    val updateAvailable: Boolean = false,
    val versionCode: Int = 0,
    val versionName: String? = null,
    val apkUrl: String? = null,
    @Json(name = "apk_url") val apkUrlSnake: String? = null,
    val cdn1Url: String? = null,
    @Json(name = "cdn1_url") val cdn1UrlSnake: String? = null,
    val githubUrl: String? = null,
    @Json(name = "github_url") val githubUrlSnake: String? = null,
    val sha256: String? = null,
    val mandatory: Boolean = false,
    val minimumSupportedVersionCode: Int = 0,
    @Json(name = "minimum_supported_version_code") val minimumSupportedVersionCodeSnake: Int = 0,
    val minimumVersionCode: Int = 0,
    @Json(name = "minimum_version_code") val minimumVersionCodeSnake: Int = 0,
    val notes: String? = null,
    val publishedAt: String? = null,
    val message: String? = null,
    override val error: String? = null,
    override val code: String? = null,
) : ApiStatus {
    val resolvedMinimumSupportedVersionCode: Int
        get() = maxOf(
            minimumSupportedVersionCode,
            minimumSupportedVersionCodeSnake,
            minimumVersionCode,
            minimumVersionCodeSnake,
        )

    val resolvedPrimaryUrl: String?
        get() = apkUrl?.takeIf { it.isNotBlank() }
            ?: apkUrlSnake?.takeIf { it.isNotBlank() }
            ?: cdn1Url?.takeIf { it.isNotBlank() }
            ?: cdn1UrlSnake?.takeIf { it.isNotBlank() }
            ?: githubUrl?.takeIf { it.isNotBlank() }
            ?: githubUrlSnake?.takeIf { it.isNotBlank() }

    val resolvedCdn1Url: String?
        get() = cdn1Url?.takeIf { it.isNotBlank() }
            ?: cdn1UrlSnake?.takeIf { it.isNotBlank() }

    val resolvedGithubUrl: String?
        get() = githubUrl?.takeIf { it.isNotBlank() }
            ?: githubUrlSnake?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
)

// ---------------------------------------------------------------------------
// Authentication, profile & QR / connected devices
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ReportLinkTicketDto(
    val rt: String = "",
    @Json(name = "expires_in") val expiresIn: Int = 0,
)

@JsonClass(generateAdapter = true)
data class UserReportRequest(
    val kind: String = "bug",
    val message: String,
    val contact: String? = null,
    val page: String? = null,
    val version: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserReportReceiptDto(
    val reference: String? = null,
    @Json(name = "issue_url") val issueUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class WebAuthnCapabilitiesDto(
    val enabled: Boolean = false,
    val origins: List<String> = emptyList(),
    val android: WebAuthnAndroidCapabilityDto? = null,
)

@JsonClass(generateAdapter = true)
data class WebAuthnAndroidCapabilityDto(
    val enabled: Boolean = false,
    @Json(name = "package") val packageName: String? = null,
    val origin: String? = null,
    @Json(name = "origin_legacy") val originLegacy: String? = null,
)

/**
 * Gate Android passkeys on the selected server's explicit capability, matching app package, and
 * an allowed origin. Prefer the exact installed signing-certificate origin when available (check
 * both current SHA-256 and legacy SHA-1 forms). This app also passes the selected HTTPS web origin
 * to Credential Manager; on that path the backend requires crossOrigin plus androidPackageName,
 * so an upload/debug signing certificate mismatch must not hide an otherwise configured feature.
 */
fun WebAuthnCapabilitiesDto.supportsAndroid(
    packageName: String,
    signingOrigins: Set<String>,
    selectedServerOrigin: String?,
): Boolean {
    val androidCapability = android ?: return false
    val advertisedAndroidOrigins = listOfNotNull(androidCapability.origin, androidCapability.originLegacy)
        .filter { it.isNotBlank() }
    val hasMatchingInstalledSigningOrigin = advertisedAndroidOrigins.any { it in signingOrigins }
    val hasAllowedSelectedWebOrigin = !selectedServerOrigin.isNullOrBlank() && selectedServerOrigin in origins

    val packageBoundWebOriginSupported =
        hasAllowedSelectedWebOrigin && androidCapability.packageName == packageName
    val originRouteSupported = if (hasMatchingInstalledSigningOrigin) {
        hasAllowedSelectedWebOrigin
    } else {
        packageBoundWebOriginSupported
    }

    return enabled &&
        androidCapability.enabled &&
        androidCapability.packageName == packageName &&
        advertisedAndroidOrigins.isNotEmpty() &&
        originRouteSupported
}

@JsonClass(generateAdapter = true)
data class WebAuthnLoginOptionsRequest(
    val username: String? = null,
)

@JsonClass(generateAdapter = true)
data class WebAuthnRegistrationOptionsRequest(
    val fingerprint: String,
    @Json(name = "device_type") val deviceType: String = "platform",
)

@JsonClass(generateAdapter = true)
data class WebAuthnOptionsResponse(
    override val success: Boolean = false,
    val options: Map<String, Any?> = emptyMap(),
    override val error: String? = null,
    override val code: String? = null,
) : ma.etherealmber.atlascommerce.core.net.ApiStatus

@JsonClass(generateAdapter = true)
data class WebAuthnCredentialVerifyRequest(
    val credential: Map<String, Any?>,
    val fingerprint: String,
    @Json(name = "device_type") val deviceType: String? = null,
    @Json(name = "device_label") val deviceLabel: String? = null,
)

@JsonClass(generateAdapter = true)
data class WebAuthnRegistrationVerifyResponse(
    override val success: Boolean = false,
    val message: String? = null,
    val label: String? = null,
    override val error: String? = null,
    override val code: String? = null,
) : ma.etherealmber.atlascommerce.core.net.ApiStatus

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
    val fingerprint: String,
    val client: String = "android",
)

@JsonClass(generateAdapter = true)
data class OtpRequest(
    val username: String,
    val password: String,
    val otp: String,
    val fingerprint: String,
    val client: String = "android",
)

@JsonClass(generateAdapter = true)
data class VerifyLinkRequest(
    val username: String,
    val otp: String,
    val sig: String,
    val exp: Long,
    val fingerprint: String,
    val client: String = "android",
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    override val success: Boolean = false,
    @Json(name = "needs_otp") val needsOtp: Boolean = false,
    val token: String? = null,
    val username: String? = null,
    val role: String? = null,
    val displayName: String? = null,
    val authMethod: String? = null,
    val message: String? = null,
    override val error: String? = null,
    override val code: String? = null,
) : ApiStatus

@JsonClass(generateAdapter = true)
data class MeDto(
    val ok: Boolean = false,
    val username: String? = null,
    val role: String? = null,
    @Json(name = "is_owner") val isOwner: Boolean = false,
    @Json(name = "display_name") val displayName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "reputationScore") val reputationScore: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    @Json(name = "display_name") val displayName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class TrustedDeviceDto(
    val fingerprint: String = "",
    val full: String = "",
    @Json(name = "last_ip") val lastIp: String? = null,
    @Json(name = "last_seen") val lastSeen: String? = null,
    @Json(name = "user_agent") val userAgent: String? = null,
    val current: Boolean = false,
) {
    /** Worker truncates `fingerprint` to 12 chars + `…` and stores full SHA-256 in `full`. */
    val revokeId: String get() = full.ifBlank { fingerprint }

    val friendlyLabel: String
        get() {
            val ua = userAgent.orEmpty()
            return when {
                ua.contains("Atlas-Android", ignoreCase = true) ||
                    ua.contains("okhttp", ignoreCase = true) -> "Atlas Android · Mobile"
                ua.contains("Android", ignoreCase = true) -> "Appareil Android"
                ua.contains("iPhone", ignoreCase = true) || ua.contains("iPad", ignoreCase = true) -> "iPhone / iPad"
                ua.contains("Windows", ignoreCase = true) && ua.contains("Edg", ignoreCase = true) -> "PC Windows · Edge"
                ua.contains("Windows", ignoreCase = true) && ua.contains("Chrome", ignoreCase = true) -> "PC Windows · Chrome"
                ua.contains("Windows", ignoreCase = true) && ua.contains("Firefox", ignoreCase = true) -> "PC Windows · Firefox"
                ua.contains("Windows", ignoreCase = true) -> "Ordinateur Windows"
                ua.contains("Macintosh", ignoreCase = true) || ua.contains("Mac OS", ignoreCase = true) -> "Mac · Navigateur"
                ua.contains("Linux", ignoreCase = true) -> "Poste Linux"
                ua.isNotBlank() -> ua.take(36)
                else -> "Session Atlas"
            }
        }
}

@JsonClass(generateAdapter = true)
data class RevokeDeviceRequest(
    val fingerprint: String,
)

@JsonClass(generateAdapter = true)
data class QrClaimRequest(
    val fingerprint: String,
    @Json(name = "user_agent") val userAgent: String,
    val confirm: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class QrSessionStatusDto(
    val status: String = "",
    val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val role: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    val message: String? = null,
    @Json(name = "needs_confirm") val needsConfirm: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class QrCompleteRequest(
    val fingerprint: String,
)

@JsonClass(generateAdapter = true)
data class QrCompleteDto(
    val token: String? = null,
    val username: String? = null,
    val role: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "trusted_device") val trustedDevice: Boolean? = null,
)

// ---------------------------------------------------------------------------
// Company / settings
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class CompanySettingsDto(
    @Json(name = "company_name") val companyName: String? = null,
    val currency: String? = null,
    @Json(name = "company_phone") val phone: String? = null,
    @Json(name = "company_email") val email: String? = null,
    @Json(name = "company_address") val address: String? = null,
    @Json(name = "company_ice") val ice: String? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "tax_rate") val taxRate: Double? = null,
)

// ---------------------------------------------------------------------------
// Dashboard
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class UnpaidDto(
    val count: Int = 0,
    val total: Double = 0.0,
    val open: Double = 0.0,
    val overdue: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class MonthlyPointDto(
    val key: String = "",
    val label: String = "",
    val sales: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class DashboardDto(
    val currency: String? = null,
    @Json(name = "currentBalance") val currentBalance: Double = 0.0,
    @Json(name = "startingBalance") val startingBalance: Double = 0.0,
    @Json(name = "stockValue") val stockValue: Double = 0.0,
    @Json(name = "productCount") val productCount: Int = 0,
    @Json(name = "lowStockCount") val lowStockCount: Int = 0,
    @Json(name = "salesReceived") val salesReceived: Double = 0.0,
    @Json(name = "expensesTotal") val expensesTotal: Double = 0.0,
    @Json(name = "refundsTotal") val refundsTotal: Double = 0.0,
    @Json(name = "purchasesPaid") val purchasesPaid: Double = 0.0,
    @Json(name = "unpaidSales") val unpaidSales: UnpaidDto = UnpaidDto(),
    @Json(name = "unpaidPurchases") val unpaidPurchases: UnpaidDto = UnpaidDto(),
    @Json(name = "recentInvoices") val recentInvoices: List<InvoiceDto> = emptyList(),
    @Json(name = "unpaidInvoiceList") val unpaidInvoiceList: List<InvoiceDto> = emptyList(),
    @Json(name = "monthlySeries") val monthlySeries: List<MonthlyPointDto> = emptyList(),
) {
    val activeCurrency: String get() = currency?.takeIf { it.isNotBlank() } ?: "MAD"
}

// ---------------------------------------------------------------------------
// Admin finance, payables & employee payroll (server-authorized endpoints)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class FinanceMonthlyDto(
    val period: String = "",
    val timezone: String = "",
    val currency: String = "MAD",
    @Json(name = "opening_balance") val openingBalance: Double = 0.0,
    @Json(name = "closing_balance") val closingBalance: Double = 0.0,
    @Json(name = "sales_received") val salesReceived: Double = 0.0,
    val refunds: Double = 0.0,
    val expenses: Double = 0.0,
    @Json(name = "payroll_paid") val payrollPaid: Double = 0.0,
    @Json(name = "supplier_paid") val supplierPaid: Double = 0.0,
    val receivables: FinanceOutstandingBucketDto = FinanceOutstandingBucketDto(),
    val payables: FinanceOutstandingBucketDto = FinanceOutstandingBucketDto(),
    val payroll: FinancePayrollBucketDto = FinancePayrollBucketDto(),
    val reconciliation: FinanceReconciliationDto = FinanceReconciliationDto(),
)

@JsonClass(generateAdapter = true)
data class FinanceStatementDto(
    val period: String = "",
    val timezone: String = "",
    val currency: String = "MAD",
    @Json(name = "as_of") val asOf: String = "",
    val status: String = "open",
    @Json(name = "opening_balance") val openingBalance: Double = 0.0,
    @Json(name = "closing_balance") val closingBalance: Double = 0.0,
    val totals: FinanceStatementTotalsDto = FinanceStatementTotalsDto(),
    val reconciliation: FinanceStatementReconciliationDto = FinanceStatementReconciliationDto(),
    val page: Int = 1,
    @Json(name = "page_size") val pageSize: Int = 50,
    val total: Int = 0,
    @Json(name = "has_more") val hasMore: Boolean = false,
    val rows: List<FinanceStatementRowDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FinancePeriodDto(
    val period: String = "",
    val timezone: String = "",
    val currency: String = "MAD",
    @Json(name = "as_of") val asOf: String = "",
    val status: String = "open",
    val inflows: FinancePeriodSplitDto = FinancePeriodSplitDto(),
    val outflows: FinancePeriodOutflowsDto = FinancePeriodOutflowsDto(),
    val owner: FinanceOwnerMovementsDto = FinanceOwnerMovementsDto(),
    @Json(name = "expense_categories") val expenseCategories: List<FinanceCategoryTotalDto> = emptyList(),
    @Json(name = "categories_truncated") val categoriesTruncated: Boolean = false,
    val statement: FinancePeriodStatementDto = FinancePeriodStatementDto(),
)

@JsonClass(generateAdapter = true)
data class FinancePeriodSplitDto(
    val total: Double = 0.0,
    val count: Int = 0,
    val cash: Double = 0.0,
    val bank: Double = 0.0,
    @Json(name = "not_recorded") val notRecorded: Double = 0.0,
    @Json(name = "by_method") val byMethod: Map<String, Double> = emptyMap(),
    @Json(name = "by_source") val bySource: Map<String, FinancePeriodSplitDto> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class FinancePeriodOutflowsDto(
    val total: Double = 0.0,
    val count: Int = 0,
    val cash: Double = 0.0,
    val bank: Double = 0.0,
    @Json(name = "not_recorded") val notRecorded: Double = 0.0,
    @Json(name = "refunds_total") val refundsTotal: Double = 0.0,
    @Json(name = "by_source") val bySource: Map<String, FinancePeriodSplitDto> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class FinanceOwnerMovementsDto(
    val contributions: FinancePeriodSplitDto = FinancePeriodSplitDto(),
    val withdrawals: FinancePeriodSplitDto = FinancePeriodSplitDto(),
    val net: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class FinanceCategoryTotalDto(
    val category: String = "",
    val total: Double = 0.0,
    val count: Int = 0,
)

@JsonClass(generateAdapter = true)
data class FinancePeriodStatementDto(
    @Json(name = "opening_balance") val openingBalance: Double = 0.0,
    val credits: Double = 0.0,
    val debits: Double = 0.0,
    @Json(name = "closing_balance") val closingBalance: Double = 0.0,
    val net: Double = 0.0,
    val balanced: Boolean = false,
    @Json(name = "consistent_with_breakdown") val consistentWithBreakdown: Boolean = false,
    val rule: String = "",
)

@JsonClass(generateAdapter = true)
data class FinanceStatementTotalsDto(
    val credits: Double = 0.0,
    val debits: Double = 0.0,
    val net: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class FinanceStatementReconciliationDto(
    val opening: Double = 0.0,
    val credits: Double = 0.0,
    val debits: Double = 0.0,
    val closing: Double = 0.0,
    val balanced: Boolean = false,
    val rule: String = "",
)

@JsonClass(generateAdapter = true)
data class FinanceStatementRowDto(
    val id: Long = 0,
    val source: String = "",
    val type: String = "",
    val label: String = "",
    val ref: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val ts: String = "",
    @Json(name = "running_balance") val runningBalance: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class CreateFinanceEntryRequest(
    val category: String,
    val description: String,
    val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "expense_date") val expenseDate: String,
    @Json(name = "vendor_name") val vendorName: String = "",
    val reference: String = "",
    val notes: String = "",
    @Json(name = "entry_type") val entryType: String,
)

@JsonClass(generateAdapter = true)
data class FinanceEntryDto(
    val id: Long = 0,
    val category: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    @Json(name = "payment_method") val paymentMethod: String = "cash",
    @Json(name = "expense_date") val expenseDate: String = "",
    @Json(name = "vendor_name") val vendorName: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    @Json(name = "entry_type") val entryType: String = "expense",
)

@JsonClass(generateAdapter = true)
data class FinanceOutstandingBucketDto(
    @Json(name = "total_outstanding") val totalOutstanding: Double = 0.0,
    @Json(name = "total_overdue") val totalOverdue: Double = 0.0,
    val count: Int = 0,
    val truncated: Boolean = false,
    val documents: List<FinanceOutstandingDocumentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FinanceOutstandingDocumentDto(
    val id: Long = 0,
    val number: String = "",
    val party: String = "",
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val remaining: Double = 0.0,
    @Json(name = "due_date") val dueDate: String? = null,
    val state: String = "unpaid",
)

@JsonClass(generateAdapter = true)
data class FinancePayrollBucketDto(
    @Json(name = "total_due") val totalDue: Double = 0.0,
    val employees: Int = 0,
)

@JsonClass(generateAdapter = true)
data class FinanceReconciliationDto(
    val source: String = "",
    @Json(name = "generated_at") val generatedAt: String = "",
    val rules: String = "",
    @Json(name = "as_of_date") val asOfDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class PayrollPageDto(
    val period: String = "",
    @Json(name = "as_of") val asOf: String = "",
    val timezone: String = "",
    val currency: String = "MAD",
    val totals: PayrollTotalsDto = PayrollTotalsDto(),
    val page: Int = 1,
    @Json(name = "page_size") val pageSize: Int = 50,
    val total: Int = 0,
    @Json(name = "has_more") val hasMore: Boolean = false,
    val rows: List<PayrollRowDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PayrollTotalsDto(
    @Json(name = "amount_due") val amountDue: Double = 0.0,
    @Json(name = "unpaid_count") val unpaidCount: Int = 0,
    val count: Int = 0,
)

@JsonClass(generateAdapter = true)
data class PayrollRowDto(
    @Json(name = "employee_id") val employeeId: Long = 0,
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "amount_due") val amountDue: Double = 0.0,
    @Json(name = "pay_period") val payPeriod: PayrollPeriodDto = PayrollPeriodDto(),
    @Json(name = "due_date") val dueDate: String? = null,
    val status: String = "unpaid",
    @Json(name = "last_payment") val lastPayment: PayrollLastPaymentDto? = null,
)

@JsonClass(generateAdapter = true)
data class PayrollPeriodDto(
    val from: String = "",
    val to: String = "",
)

@JsonClass(generateAdapter = true)
data class PayrollLastPaymentDto(
    val amount: Double = 0.0,
    @Json(name = "paid_at") val paidAt: String = "",
    @Json(name = "period_label") val periodLabel: String = "",
)

// ---------------------------------------------------------------------------
// Products, scan requests & needed items
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: Long = 0,
    val name: String = "",
    val sku: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val price: Double = 0.0,
    @Json(name = "stock_quantity") val stock: Double = 0.0,
    @Json(name = "low_stock_threshold") val lowStockThresholdRaw: Double? = null,
    @Json(name = "track_inventory") val trackInventoryRaw: Int? = 1,
    @Json(name = "is_active") val isActiveRaw: Int? = 1,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "variant_label") val variantLabel: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
) {
    val lowStockThreshold: Double get() = lowStockThresholdRaw ?: 0.0

    val trackInventory: Boolean get() = (trackInventoryRaw ?: 1) != 0
    val isActive: Boolean get() = (isActiveRaw ?: 1) != 0

    val isLowStock: Boolean get() = trackInventory && stock <= lowStockThreshold
    val isOutOfStock: Boolean get() = trackInventory && stock <= 0.0

    /** Restock units required to climb back safely above threshold. */
    val neededQuantity: Double
        get() {
            if (!trackInventory || stock > lowStockThreshold) return 0.0
            val target = if (lowStockThreshold > 0.0) lowStockThreshold else 1.0
            return ceil((target - stock).coerceAtLeast(1.0))
        }

    val subtitle: String
        get() = listOfNotNull(
            sku?.takeIf { it.isNotBlank() },
            barcode?.takeIf { it.isNotBlank() },
            category?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

@JsonClass(generateAdapter = true)
data class CreateProductRequest(
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val category: String? = null,
    val unit: String = "unité",
    val price: Double = 0.0,
    @Json(name = "stock_quantity") val stockQuantity: Double = 0.0,
    @Json(name = "low_stock_threshold") val lowStockThreshold: Double = 5.0,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProductCreationRequestDto(
    val id: Long = 0,
    val code: String = "",
    val name: String? = null,
    val sku: String? = null,
    val unit: String? = null,
    val quantity: Double? = null,
    val category: String? = null,
    val note: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    val status: String = "open",
    @Json(name = "created_by") val createdBy: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubmitProductRequestBody(
    val code: String,
    val name: String,
    val sku: String? = null,
    val unit: String = "unité",
    val quantity: Double = 1.0,
    val category: String? = null,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class CloseProductRequestBody(
    val status: String,
    @Json(name = "product_id") val productId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class StockAdjustRequest(
    val quantity: Double,
    /** "in" (entrée), "out" (sortie) or "adjustment". */
    val type: String = "adjustment",
    val reason: String? = null,
)

// ---------------------------------------------------------------------------
// Notifications, alerts & mobile actions
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class AlertItemDto(
    val kind: String = "info",
    val label: String = "",
    val severity: String? = null,
    val href: String? = null,
    val count: Int? = null,
)

@JsonClass(generateAdapter = true)
data class MobileActionMetaDto(
    @Json(name = "target_role") val targetRole: String? = null,
    @Json(name = "sender_role") val senderRole: String? = null,
    val priority: String? = null,
    val category: String? = null,
    @Json(name = "proposed_stock") val proposedStock: Double? = null,
    @Json(name = "create_product_body") val createProductBody: CreateProductRequest? = null,
)

@JsonClass(generateAdapter = true)
data class MobileActionDto(
    val id: Long = 0,
    val type: String = "scan",
    val title: String = "",
    val detail: String? = null,
    val status: String = "pending",
    val source: String? = null,
    val barcode: String? = null,
    @Json(name = "product_id") val productId: Long? = null,
    @Json(name = "created_by") val createdBy: String? = null,
    @Json(name = "completed_by") val completedBy: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "meta_json") val metaJson: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
) {
    /** Extracts `target_role` from `meta_json` without reflection and without crashing on arbitrary JSON. */
    val resolvedTargetRole: String?
        get() {
            val raw = metaJson ?: return null
            val match = TARGET_ROLE_REGEX.find(raw) ?: return null
            return match.groupValues[1].trim().lowercase().takeIf { it.isNotEmpty() }
        }

    val isUrgent: Boolean
        get() = metaJson?.contains("\"priority\":\"urgent\"", ignoreCase = true) == true ||
            title.contains("[URGENT]", ignoreCase = true)

    val isRejectedOrCancelled: Boolean
        get() = status.equals("cancelled", ignoreCase = true) ||
            status.equals("rejected", ignoreCase = true)

    val isCompleted: Boolean
        get() = status.equals("done", ignoreCase = true) ||
            status.equals("completed", ignoreCase = true)

    val isPending: Boolean
        get() = status.equals("pending", ignoreCase = true)

    private companion object {
        val TARGET_ROLE_REGEX = Regex("\"target_role\"\\s*:\\s*\"([^\"]+)\"")
    }
}

@JsonClass(generateAdapter = true)
data class MobileActionStatsDto(
    val pending: Int = 0,
    val today: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CreateMobileActionRequest(
    val type: String,
    val title: String,
    val detail: String? = null,
    val barcode: String? = null,
    @Json(name = "product_id") val productId: Long? = null,
    @Json(name = "created_by") val createdBy: String = "mobile",
    val source: String = "android",
    val meta: MobileActionMetaDto? = null,
)

@JsonClass(generateAdapter = true)
data class CompleteMobileActionRequest(
    @Json(name = "apply_stock") val applyStock: Boolean = true,
    @Json(name = "completed_by") val completedBy: String? = null,
    @Json(name = "product_id") val productId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class RejectMobileActionRequest(
    val reason: String,
)

// ---------------------------------------------------------------------------
// Atlas Agent IA
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class AssistStatusDto(
    val configured: Boolean = false,
    val provider: String? = null,
    val model: String? = null,
    val hint: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssistNextActionItemDto(
    val priority: Int = 0,
    val title: String = "",
    val detail: String = "",
    val href: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssistNextActionsDto(
    val items: List<AssistNextActionItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AssistChatRequest(
    val message: String,
    @Json(name = "user_message") val userMessage: String,
    val task: String = "general",
    val timezone: String = "Africa/Casablanca",
    @Json(name = "thread_id") val threadId: String = "live",
    @Json(name = "page_path") val pagePath: String? = "/agent",
    @Json(name = "page_title") val pageTitle: String? = "Atlas Agent",
    val source: String = "android",
    val images: List<String> = emptyList(),
    val documents: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AgentPolicyDto(
    val blocked: Boolean = false,
    val isAdmin: Boolean = false,
    val dailyUsed: Long = 0L,
    val weeklyUsed: Long = 0L,
    val dailyLimit: Long = 25_000L,
    val weeklyLimit: Long = 100_000L,
    val dailyResetsAt: String? = null,
    val weeklyResetsAt: String? = null,
) {
    val safeDailyLimit: Long get() = dailyLimit.coerceAtLeast(1L)
    val safeWeeklyLimit: Long get() = weeklyLimit.coerceAtLeast(1L)

    val dailyFraction: Float
        get() = (dailyUsed.toDouble() / safeDailyLimit.toDouble()).toFloat().coerceIn(0f, 1f)

    val weeklyFraction: Float
        get() = (weeklyUsed.toDouble() / safeWeeklyLimit.toDouble()).toFloat().coerceIn(0f, 1f)

    val isDailyExhausted: Boolean get() = !isAdmin && dailyLimit > 0L && dailyUsed >= dailyLimit
    val isWeeklyExhausted: Boolean get() = !isAdmin && weeklyLimit > 0L && weeklyUsed >= weeklyLimit
    val isExhausted: Boolean get() = blocked || isDailyExhausted || isWeeklyExhausted
}

@JsonClass(generateAdapter = true)
data class AgentThreadSummaryDto(
    val id: String = "",
    val title: String = "Sans titre",
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "message_count") val messageCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AgentThreadMessageDto(
    val role: String = "user",
    val text: String = "",
    val ms: Long? = null,
)

@JsonClass(generateAdapter = true)
data class AgentThreadDetailDto(
    val title: String? = null,
    val messages: List<AgentThreadMessageDto> = emptyList(),
    @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class PutAgentThreadRequest(
    val title: String,
    val messages: List<AgentThreadMessageDto>,
)

@JsonClass(generateAdapter = true)
data class AssistToolTraceDto(
    val name: String = "",
    val ok: Boolean = true,
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssistProposalDto(
    @Json(name = "proposal_id") val proposalId: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val action: String? = null,
    val kind: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssistChatResponseDto(
    val reply: String = "",
    val model: String? = null,
    val provider: String? = null,
    @Json(name = "local_time") val localTime: String? = null,
    @Json(name = "tool_trace") val toolTrace: List<AssistToolTraceDto> = emptyList(),
    val proposals: List<AssistProposalDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AssistApproveRequest(
    @Json(name = "proposal_id") val proposalId: String,
)

@JsonClass(generateAdapter = true)
data class AssistApproveDto(
    val queued: Boolean = false,
    val message: String? = null,
)

// ---------------------------------------------------------------------------
// Clients
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ClientDto(
    val id: Long = 0,
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val ice: String? = null,
    @Json(name = "balance_due") val balanceDue: Double = 0.0,
)

// ---------------------------------------------------------------------------
// Invoices / quick sales
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class InvoiceDto(
    val id: Long = 0,
    val number: String? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "issue_date") val issueDate: String? = null,
    @Json(name = "due_date") val dueDate: String? = null,
    val total: Double = 0.0,
    @Json(name = "amount_paid") val amountPaidRaw: Double? = null,
    val status: String? = null,
    @Json(name = "doc_kind") val docKind: String? = null,
) {
    val amountPaid: Double get() = amountPaidRaw ?: 0.0
    val remaining: Double get() = (total - amountPaid).coerceAtLeast(0.0)
    val isQuickSale: Boolean
        get() = docKind == "quick_sale" || docKind == "ticket" || number?.startsWith("VR") == true
}

@JsonClass(generateAdapter = true)
data class QuickSaleItemDto(
    @Json(name = "product_id") val productId: Long,
    @Json(name = "product_name") val productName: String,
    val quantity: Double,
    @Json(name = "unit_price") val unitPrice: Double,
    @Json(name = "list_price") val listPrice: Double = unitPrice,
    @Json(name = "tax_rate") val taxRate: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class QuickSaleRequest(
    @Json(name = "client_name") val clientName: String,
    @Json(name = "client_phone") val clientPhone: String? = null,
    @Json(name = "issue_date") val issueDate: String,
    val status: String = "sent",
    @Json(name = "quick_sale") val quickSale: Boolean = true,
    @Json(name = "doc_kind") val docKind: String = "quick_sale",
    @Json(name = "tax_exempt") val taxExempt: Boolean = true,
    val notes: String? = null,
    val items: List<QuickSaleItemDto>,
)

@JsonClass(generateAdapter = true)
data class CreatedInvoiceDto(
    val id: Long = 0,
    val number: String? = null,
    val total: Double = 0.0,
    val status: String? = null,
    @Json(name = "client_name") val clientName: String? = null,
)
