package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Json
import ma.etherealmber.atlascommerce.core.net.ApiStatus

// ---------------------------------------------------------------------------
// Health / server discovery
// ---------------------------------------------------------------------------

data class HealthDto(
    val ok: Boolean = false,
    val d1: Boolean = false,
    val kv: Boolean = false,
    val r2: Boolean = false,
    val cdn: String? = null,
    val environment: String? = null,
    val time: String? = null,
)

// ---------------------------------------------------------------------------
// Authentication
// ---------------------------------------------------------------------------

data class LoginRequest(
    val username: String,
    val password: String,
    val fingerprint: String,
    val client: String = "android",
)

data class OtpRequest(
    val username: String,
    val password: String,
    val otp: String,
    val fingerprint: String,
    val client: String = "android",
)

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

// ---------------------------------------------------------------------------
// Company / settings
// ---------------------------------------------------------------------------

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

data class UnpaidDto(
    val count: Int = 0,
    val total: Double = 0.0,
    val open: Double = 0.0,
    val overdue: Double = 0.0,
)

data class MonthlyPointDto(
    val key: String = "",
    val label: String = "",
    val sales: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
)

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
// Products
// ---------------------------------------------------------------------------

data class ProductDto(
    val id: Long = 0,
    val name: String = "",
    val sku: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val price: Double = 0.0,
    @Json(name = "stock_quantity") val stock: Double = 0.0,
    @Json(name = "low_stock_threshold") val lowStockThreshold: Double = 0.0,
    @Json(name = "track_inventory") val trackInventory: Boolean = true,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "variant_label") val variantLabel: String? = null,
) {
    val isLowStock: Boolean get() = trackInventory && stock <= lowStockThreshold
    val isOutOfStock: Boolean get() = trackInventory && stock <= 0.0
    val subtitle: String
        get() = listOfNotNull(
            sku?.takeIf { it.isNotBlank() },
            barcode?.takeIf { it.isNotBlank() },
            category?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

// ---------------------------------------------------------------------------
// Clients
// ---------------------------------------------------------------------------

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

data class InvoiceDto(
    val id: Long = 0,
    val number: String? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "issue_date") val issueDate: String? = null,
    @Json(name = "due_date") val dueDate: String? = null,
    val total: Double = 0.0,
    @Json(name = "amount_paid") val amountPaid: Double = 0.0,
    val status: String? = null,
    @Json(name = "doc_kind") val docKind: String? = null,
) {
    val remaining: Double get() = (total - amountPaid).coerceAtLeast(0.0)
    val isQuickSale: Boolean
        get() = docKind == "quick_sale" || docKind == "ticket" || number?.startsWith("VR") == true
}

data class QuickSaleItemDto(
    @Json(name = "product_id") val productId: Long,
    @Json(name = "product_name") val productName: String,
    val quantity: Double,
    @Json(name = "unit_price") val unitPrice: Double,
    @Json(name = "list_price") val listPrice: Double = unitPrice,
    @Json(name = "tax_rate") val taxRate: Double = 0.0,
)

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

data class CreatedInvoiceDto(
    val id: Long = 0,
    val number: String? = null,
    val total: Double = 0.0,
    val status: String? = null,
    @Json(name = "client_name") val clientName: String? = null,
)
